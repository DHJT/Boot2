package tech.dhjt.boot3.service.flowable;

import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.dhjt.boot3.event.NotificationEvent;
import tech.dhjt.boot3.model.po.Notification;
import tech.dhjt.boot3.model.po.User;
import tech.dhjt.boot3.service.UserService;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 审批管理服务 — 提供改签（转办临时/永久）、加签、去签、中止暂停、终止、强行通过等管理操作
 *
 * 所有操作均附带任务提醒通知机制：
 * - 改签/加签/去签 → 提醒任务办理人或候选人
 * - 中止暂停/终止 → 额外提醒流程提交人
 */
@RequiredArgsConstructor
@Service
public class ApprovalAdminService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalAdminService.class);

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final ProcessCommonService processCommonService;

    // =====================================================================
    //  改签他人（转办）
    // =====================================================================

    /**
     * 转办（临时改签）- 将任务转给其他人处理，保留原处理人跟踪
     * 委派模式：原处理人仍可追踪任务，被委派人完成后任务回到原处理人
     */
    @Transactional
    public void transferTemporary(String taskId, String targetUserId) {
        Task task = assertTaskExists(taskId);
        User targetUser = userService.getUserById(Long.valueOf(targetUserId));
        if (targetUser == null) {
            throw new RuntimeException("目标用户不存在: " + targetUserId);
        }

        String originalAssignee = task.getAssignee();
        String processInstanceId = task.getProcessInstanceId();

        // 临时改签：设置 owner 为原处理人，assignee 为被委派人
        taskService.setOwner(taskId, originalAssignee);
        taskService.setAssignee(taskId, targetUser.getName());

        // 记录转办信息到流程变量
        runtimeService.setVariable(processInstanceId, "tempTransfer_from_" + taskId, originalAssignee);
        runtimeService.setVariable(processInstanceId, "tempTransfer_to_" + taskId, targetUser.getName());
        runtimeService.setVariable(processInstanceId, "tempTransfer_time_" + taskId, new Date());

        log.info("临时改签成功: 任务 {} 从 {} 临时转给 {}, 完成后将回到原处理人",
                taskId, originalAssignee, targetUser.getName());

        processCommonService.recordOperationLog(processInstanceId, taskId, "TEMP_TRANSFER",
                String.format("临时改签: %s → %s(临时)", originalAssignee, targetUser.getName()));

        // 发送通知
        sendTaskNotification(targetUser, "任务临时改签通知",
                "您被临时指定处理任务: " + task.getName() + "，处理后任务将回到原处理人",
                "TASK_TRANSFER", processInstanceId, taskId);

        // 通知原处理人
        notifyOriginalAssignee(task, originalAssignee, "任务已临时改签",
                "您的任务 " + task.getName() + " 已临时转交 " + targetUser.getName() + " 处理",
                "TASK_TRANSFER", processInstanceId, taskId);
    }

    /**
     * 永久改签 - 将任务彻底转给其他人处理（当前用户不再拥有该任务）
     */
    @Transactional
    public void transferPermanent(String taskId, String targetUserId) {
        Task task = assertTaskExists(taskId);
        User targetUser = userService.getUserById(Long.valueOf(targetUserId));
        if (targetUser == null) {
            throw new RuntimeException("目标用户不存在: " + targetUserId);
        }

        String originalAssignee = task.getAssignee();
        String processInstanceId = task.getProcessInstanceId();

        // 永久改签：直接设置新的 assignee
        taskService.setAssignee(taskId, targetUser.getName());

        // 记录转办信息到流程变量
        runtimeService.setVariable(processInstanceId, "permanentTransfer_from_" + taskId, originalAssignee);
        runtimeService.setVariable(processInstanceId, "permanentTransfer_to_" + taskId, targetUser.getName());
        runtimeService.setVariable(processInstanceId, "permanentTransfer_time_" + taskId, new Date());

        log.info("永久改签成功: 任务 {} 从 {} 永久转给 {}", taskId, originalAssignee, targetUser.getName());

        processCommonService.recordOperationLog(processInstanceId, taskId, "PERMANENT_TRANSFER",
                String.format("永久改签: %s → %s(永久)", originalAssignee, targetUser.getName()));

        // 发送通知
        sendTaskNotification(targetUser, "任务转办通知",
                "您收到一个转办任务: " + task.getName() + "，请及时处理",
                "TASK_TRANSFER", processInstanceId, taskId);

        // 通知原处理人
        notifyOriginalAssignee(task, originalAssignee, "任务已转办",
                "您的任务 " + task.getName() + " 已转交给 " + targetUser.getName(),
                "TASK_TRANSFER", processInstanceId, taskId);
    }

    // =====================================================================
    //  加签（增加审批人）
    // =====================================================================

    /**
     * 加签 - 在当前审批节点增加额外的审批人（候选用户）
     */
    @Transactional
    public void addApprover(String taskId, String newUserId) {
        Task task = assertTaskExists(taskId);
        User newUser = userService.getUserById(Long.valueOf(newUserId));
        if (newUser == null) {
            throw new RuntimeException("用户不存在: " + newUserId);
        }

        String processInstanceId = task.getProcessInstanceId();

        // 添加候选用户（加签）
        taskService.addUserIdentityLink(taskId, newUser.getName(), IdentityLinkType.CANDIDATE);

        log.info("加签成功: 任务 {} 增加审批人 {}", taskId, newUser.getName());

        processCommonService.recordOperationLog(processInstanceId, taskId, "ADD_APPROVER",
                String.format("加签 +%s", newUser.getName()));

        // 发送通知
        sendTaskNotification(newUser, "新审批任务",
                "您被添加为审批人: " + task.getName() + "，请及时处理",
                "TASK_ADD_APPROVER", processInstanceId, taskId);

        // 通知原审批人
        String assignee = task.getAssignee();
        if (assignee != null) {
            notifyAssignee(assignee, "审批人已增加",
                    "任务 " + task.getName() + " 已增加审批人: " + newUser.getName(),
                    "TASK_ADD_APPROVER", processInstanceId, taskId);
        }
    }

    // =====================================================================
    //  去签（移除审批人）
    // =====================================================================

    /**
     * 去签 - 移除当前审批节点的某个候选审批人/审批人
     */
    @Transactional
    public void removeApprover(String taskId, String userId) {
        Task task = assertTaskExists(taskId);
        User user = userService.getUserById(Long.valueOf(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }

        String processInstanceId = task.getProcessInstanceId();
        String assignee = task.getAssignee();

        // 如果该用户是当前审批人，先取消认领
        if (user.getName().equals(assignee)) {
            taskService.unclaim(taskId);
            log.info("去签: 取消当前审批人 {} 的认领", user.getName());
        }

        // 移除候选用户身份链接
        taskService.deleteUserIdentityLink(taskId, user.getName(), IdentityLinkType.CANDIDATE);

        log.info("去签成功: 任务 {} 移除审批人 {}", taskId, user.getName());

        processCommonService.recordOperationLog(processInstanceId, taskId, "REMOVE_APPROVER",
                String.format("去签 -%s", user.getName()));

        // 通知被移除的用户
        sendTaskNotification(user, "审批任务已移除",
                "您已被移出审批任务: " + task.getName(),
                "TASK_REMOVE_APPROVER", processInstanceId, taskId);

        // 通知当前审批人（如果存在）
        Task currentTask = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (currentTask != null && currentTask.getAssignee() != null) {
            notifyAssignee(currentTask.getAssignee(), "审批人已移除",
                    "任务 " + task.getName() + " 已移除审批人: " + user.getName(),
                    "TASK_REMOVE_APPROVER", processInstanceId, taskId);
        }
    }

    // =====================================================================
    //  管理接口
    // =====================================================================

    /**
     * 中止/暂停流程实例 — 额外提醒提交人
     */
    @Transactional
    public void suspendProcess(String processInstanceId, String reason) {
        runtimeService.suspendProcessInstanceById(processInstanceId);
        log.info("流程已暂停: instanceId={}, reason={}", processInstanceId, reason);

        processCommonService.recordOperationLog(processInstanceId, null, "SUSPEND",
                "流程暂停: " + reason);

        // 提醒提交人
        notifySubmitter(processInstanceId, "流程已暂停",
                "您的流程已被暂停，原因: " + reason,
                "PROCESS_SUSPEND");
    }

    /**
     * 恢复已暂停的流程实例
     */
    @Transactional
    public void activateProcess(String processInstanceId) {
        runtimeService.activateProcessInstanceById(processInstanceId);
        log.info("流程已恢复: instanceId={}", processInstanceId);

        processCommonService.recordOperationLog(processInstanceId, null, "ACTIVATE",
                "流程已恢复");

        // 提醒任务办理人
        notifyCurrentTaskAssignees(processInstanceId, "流程已恢复",
                "流程已恢复，请继续处理审批任务",
                "PROCESS_ACTIVATE");
    }

    /**
     * 终止流程实例 — 额外提醒提交人
     */
    @Transactional
    public void terminateProcess(String processInstanceId, String reason) {
        // 先设置终止原因到流程变量，供监听器读取
        runtimeService.setVariable(processInstanceId, "terminationReason", reason);
        runtimeService.setVariable(processInstanceId, "terminationTime", new Date());
        runtimeService.setVariable(processInstanceId, "terminationCandidate", "ADMIN");

        // 删除流程实例
        runtimeService.deleteProcessInstance(processInstanceId, reason);

        log.info("流程已终止: instanceId={}, reason={}", processInstanceId, reason);

        processCommonService.recordOperationLog(processInstanceId, null, "TERMINATE",
                "流程终止(管理员): " + reason);

        // 提醒提交人（GlobalProcessEventListener 中的 handleProcessCancelled 也会通知，
        // 但这里额外发送一条管理操作通知）
        notifySubmitter(processInstanceId, "流程已被管理员终止",
                "管理员已终止您的流程，原因: " + reason,
                "PROCESS_TERMINATE");
    }

    /**
     * 强行通过某个用户任务（管理操作）
     */
    @Transactional
    public void forceCompleteTask(String taskId, String comment) {
        Task task = assertTaskExists(taskId);
        String processInstanceId = task.getProcessInstanceId();

        // 设置管理员审批通过的变量
        runtimeService.setVariable(processInstanceId, "approved", true);
        runtimeService.setVariable(processInstanceId, "approval", "approved");
        runtimeService.setVariable(processInstanceId, "comment", "管理员强制通过: " + (comment != null ? comment : ""));

        // 管理员直接完成任务
        taskService.complete(taskId);

        log.info("管理员强制通过: taskId={}, processInstanceId={}", taskId, processInstanceId);

        processCommonService.recordOperationLog(processInstanceId, taskId, "FORCE_COMPLETE",
                "管理员强制通过: " + (comment != null ? comment : ""));

        // 提醒任务办理人
        String assignee = task.getAssignee();
        if (assignee != null) {
            notifyAssignee(assignee, "任务已被管理员强制通过",
                    "任务 " + task.getName() + " 已被管理员强制通过" +
                            (comment != null ? "，原因: " + comment : ""),
                    "FORCE_COMPLETE", processInstanceId, taskId);
        }

        // 提醒候选人
        notifyCandidateGroups(task, "任务已被管理员强制通过",
                "任务 " + task.getName() + " 已被管理员强制通过",
                "FORCE_COMPLETE");
    }

    // =====================================================================
    //  内部工具方法
    // =====================================================================

    private Task assertTaskExists(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new RuntimeException("任务不存在: " + taskId);
        return task;
    }

    /**
     * 通知原处理人
     */
    private void notifyOriginalAssignee(Task task, String originalAssignee,
                                         String title, String content,
                                         String type, String processInstanceId, String taskId) {
        if (originalAssignee != null) {
            User originalUser = userService.getUserByUsername(originalAssignee);
            if (originalUser != null) {
                sendNotification(originalUser, title, content, type, processInstanceId, taskId);
            }
        }
    }

    /**
     * 根据用户名通知审批人
     */
    private void notifyAssignee(String assignee, String title, String content,
                                 String type, String processInstanceId, String taskId) {
        List<User> users = userService.getUsersByGroup(assignee);
        if (users.isEmpty()) {
            User user = userService.getUserByUsername(assignee);
            if (user != null) {
                sendNotification(user, title, content, type, processInstanceId, taskId);
            }
        } else {
            for (User user : users) {
                sendNotification(user, title, content, type, processInstanceId, taskId);
            }
        }
    }

    /**
     * 通知候选人组
     */
    private void notifyCandidateGroups(Task task, String title, String content, String type) {
        var identityLinks = taskService.getIdentityLinksForTask(task.getId());
        for (var link : identityLinks) {
            if (link.getGroupId() != null) {
                List<User> groupUsers = userService.getUsersByGroup(link.getGroupId());
                for (User user : groupUsers) {
                    sendNotification(user, title, content, type,
                            task.getProcessInstanceId(), task.getId());
                }
            }
        }
    }

    /**
     * 通知流程提交人
     */
    private void notifySubmitter(String processInstanceId, String title, String content,
                                  String type) {
        try {
            Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
            String applicantName = (String) variables.get("applicantName");
            String initiator = (String) variables.get("initiator");
            String submitter = applicantName != null ? applicantName : initiator;

            if (submitter != null) {
                User submitterUser = userService.getUserByUsername(submitter);
                if (submitterUser != null) {
                    sendNotification(submitterUser, title, content, type, processInstanceId, null);
                }
            }
        } catch (Exception e) {
            log.warn("通知提交人失败: {}", e.getMessage());
        }
    }

    /**
     * 通知当前任务的所有办理人
     */
    private void notifyCurrentTaskAssignees(String processInstanceId, String title,
                                             String content, String type) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        for (Task task : tasks) {
            String assignee = task.getAssignee();
            if (assignee != null) {
                User user = userService.getUserByUsername(assignee);
                if (user != null) {
                    sendNotification(user, title, content, type, processInstanceId, task.getId());
                }
            }
            // 也通知候选组
            notifyCandidateGroups(task, title, content, type);
        }
    }

    /**
     * 发送通知（使用 Spring Event + 持久化）
     */
    private void sendTaskNotification(User user, String title, String content,
                                       String type, String processInstanceId, String taskId) {
        if (user != null) {
            sendNotification(user, title, content, type, processInstanceId, taskId);
        }
    }

    private void sendNotification(User user, String title, String content,
                                   String type, String processInstanceId, String taskId) {
        Notification notification = Notification.builder()
                .userId(user.getId())
                .username(user.getName())
                .title(title)
                .content(content)
                .type(type)
                .read(false)
                .processInstanceId(processInstanceId)
                .taskId(taskId)
                .createTime(LocalDateTime.now())
                .build();

        eventPublisher.publishEvent(new NotificationEvent(this, notification));
    }
}