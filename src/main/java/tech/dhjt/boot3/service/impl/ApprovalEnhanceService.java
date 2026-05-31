package tech.dhjt.boot3.service.impl;

import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.dhjt.boot3.event.NotificationEvent;
import tech.dhjt.boot3.model.po.Notification;
import tech.dhjt.boot3.model.po.User;
import tech.dhjt.boot3.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 审批增强服务 - 支持逐级回退、驳回重提、转办、委派、加人
 */
@RequiredArgsConstructor
@Service
public class ApprovalEnhanceService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalEnhanceService.class);

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== 逐级回退 ====================

    /**
     * 逐级回退 - 回退到上一个审批人/组
     * 将当前任务回退到上一级已完成的任务节点，并重置其审批人为候选人或指定审批人
     */
    @Transactional
    public void backToPrevious(String taskId, String comment) {
        Task currentTask = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (currentTask == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        String processInstanceId = currentTask.getProcessInstanceId();

        // 获取当前任务的前一个已完成任务（按结束时间倒序，排除当前任务定义key相同的节点）
        List<HistoricTaskInstance> completedTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();

        // 找到上一个不同节点的已完成任务
        HistoricTaskInstance previousTask = null;
        String currentDefKey = currentTask.getTaskDefinitionKey();
        for (HistoricTaskInstance ht : completedTasks) {
            if (!ht.getTaskDefinitionKey().equals(currentDefKey)) {
                previousTask = ht;
                break;
            }
        }

        if (previousTask == null) {
            throw new RuntimeException("没有找到上一级审批节点，无法回退");
        }

        // 获取回退目标节点的候选组/候选人
        List<String> candidateGroups = new ArrayList<>();
        List<String> candidateUsers = new ArrayList<>();

        // 从历史任务获取之前的审批人
        if (previousTask.getAssignee() != null) {
            candidateUsers.add(previousTask.getAssignee());
        } else {
            // 获取历史的候选人信息
//            List<IdentityLinkInfo> identityLinks = historyService.getHistoricIdentityLinksForTask(previousTask.getId());
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForTask(previousTask.getId());
            for (IdentityLinkInfo link : identityLinks) {
                if (link.getGroupId() != null) {
                    candidateGroups.add(link.getGroupId());
                }
                if (link.getUserId() != null) {
                    candidateUsers.add(link.getUserId());
                }
            }
        }

        // 记录回退节点信息到流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("backToPreviousComment", comment);
        variables.put("backToPreviousTaskKey", previousTask.getTaskDefinitionKey());
        variables.put("backToPreviousTime", new Date());

        // 使用 Flowable 的 changeActivityId 实现回退（需要启用流程实例的修改）
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(currentTask.getTaskDefinitionKey(), previousTask.getTaskDefinitionKey())
                .changeState();

        log.info("逐级回退成功: 任务 {} 从 {} 回退到 {}，意见: {}",
                taskId, currentTask.getTaskDefinitionKey(), previousTask.getTaskDefinitionKey(), comment);

        // 发送通知
        sendBackNotification(processInstanceId, previousTask, comment, "逐级回退");
    }

    // ==================== 驳回重提 ====================

    /**
     * 驳回重提 - 回退到提交人节点（流程发起人）
     */
    @Transactional
    public void rejectToSubmitter(String taskId, String comment) {
        Task currentTask = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (currentTask == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        String processInstanceId = currentTask.getProcessInstanceId();

        // 获取流程发起人信息
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        // 获取流程变量中的 initiator/applicantName 作为提交人
        String submitter = (String) runtimeService.getVariable(processInstanceId, "initiator");
        if (submitter == null) {
            submitter = (String) runtimeService.getVariable(processInstanceId, "applicantName");
        }

        // 找到提交申请节点的 taskDefinitionKey（通常是第一个用户任务）
        // 先获取流程定义中的起始任务
        List<HistoricTaskInstance> allTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime().asc()
                .list();

        String submitTaskDefKey = null;
        if (!allTasks.isEmpty()) {
            // 第一个用户任务通常是提交申请任务
            submitTaskDefKey = allTasks.get(0).getTaskDefinitionKey();
        } else {
            // 如果没有历史任务，尝试从当前运行中的任务判断
            List<Task> runningTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .orderByTaskCreateTime().asc()
                    .list();
            if (!runningTasks.isEmpty()) {
                submitTaskDefKey = runningTasks.get(0).getTaskDefinitionKey();
            }
        }

        if (submitTaskDefKey == null) {
            throw new RuntimeException("无法找到提交节点，驳回重提失败");
        }

        // 设置流程变量标识被驳回
        runtimeService.setVariable(processInstanceId, "rejected", true);
        runtimeService.setVariable(processInstanceId, "rejectComment", comment);
        runtimeService.setVariable(processInstanceId, "rejectTime", new Date());

        // 移动当前活动到提交节点
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(currentTask.getTaskDefinitionKey(), submitTaskDefKey)
                .changeState();

        log.info("驳回重提成功: 任务 {} 已回退到提交节点 {}，意见: {}",
                taskId, submitTaskDefKey, comment);

        // 发送通知给提交人
        if (submitter != null) {
            List<User> users = userRepository.findByName(submitter).stream().toList();
            if (!users.isEmpty()) {
                User submitUser = users.get(0);
                sendNotification(submitUser.getId(), submitter,
                        "流程被驳回", "您的申请被驳回，请修改后重新提交。意见: " + comment,
                        "REJECT", processInstanceId, taskId);
            }
        }
    }

    // ==================== 转办（Transfer） ====================

    /**
     * 转办 - 将任务转给其他人处理（当前用户不再拥有该任务）
     */
    @Transactional
    public void transferTask(String taskId, String targetUserId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        User targetUser = userRepository.findById(Long.valueOf(targetUserId))
                .orElseThrow(() -> new RuntimeException("目标用户不存在: " + targetUserId));

        String originalAssignee = task.getAssignee();

        // 转办：直接设置新的 assignee
        taskService.setAssignee(taskId, targetUser.getName());

        // 记录转办信息到流程变量
        String processInstanceId = task.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, "transferFrom_" + taskId, originalAssignee);
        runtimeService.setVariable(processInstanceId, "transferTo_" + taskId, targetUser.getName());
        runtimeService.setVariable(processInstanceId, "transferTime_" + taskId, new Date());

        log.info("转办成功: 任务 {} 从 {} 转给 {}", taskId, originalAssignee, targetUser.getName());

        // 发送通知
        sendNotification(targetUser.getId(), targetUser.getName(),
                "任务转办通知", "您收到一个转办任务: " + task.getName(),
                "TASK_TRANSFER", processInstanceId, taskId);

        // 通知原处理人
        if (originalAssignee != null) {
            List<User> originalUsers = userRepository.findByName(originalAssignee);
            if (!originalUsers.isEmpty()) {
                sendNotification(originalUsers.get(0).getId(), originalAssignee,
                        "任务已转办", "您已将任务 " + task.getName() + " 转交给 " + targetUser.getName(),
                        "TASK_TRANSFER", processInstanceId, taskId);
            }
        }
    }

    // ==================== 委派（Delegate） ====================

    /**
     * 委派 - 将任务委派给其他人处理，但保留跟踪权限，委派完成后任务回到委派人
     */
    @Transactional
    public void delegateTask(String taskId, String delegateUserId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        User delegateUser = userRepository.findById(Long.valueOf(delegateUserId))
                .orElseThrow(() -> new RuntimeException("被委派人不存在: " + delegateUserId));

        String owner = task.getAssignee();

        // 委派：设置 owner 为原处理人，assignee 为被委派人
        taskService.setOwner(taskId, owner);
        taskService.setAssignee(taskId, delegateUser.getName());

        log.info("委派成功: 任务 {} 由 {} 委派给 {} 处理", taskId, owner, delegateUser.getName());

        String processInstanceId = task.getProcessInstanceId();

        // 发送通知
        sendNotification(delegateUser.getId(), delegateUser.getName(),
                "任务委派通知", "您被委派处理任务: " + task.getName() + "，请及时处理",
                "TASK_DELEGATE", processInstanceId, taskId);

        if (owner != null) {
            List<User> ownerUsers = userRepository.findByName(owner);
            if (!ownerUsers.isEmpty()) {
                sendNotification(ownerUsers.get(0).getId(), owner,
                        "任务已委派", "您已将任务 " + task.getName() + " 委派给 " + delegateUser.getName() + " 处理",
                        "TASK_DELEGATE", processInstanceId, taskId);
            }
        }
    }

    /**
     * 完成委派任务 - 被委派人完成后任务回到原处理人
     */
    @Transactional
    public void resolveTask(String taskId, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        String owner = task.getOwner();
        if (owner != null) {
            // 这是委派任务，解析回原处理人
            taskService.resolveTask(taskId, variables);
            log.info("委派任务解析完成: {}，任务回到处理人: {}", taskId, owner);
        } else {
            // 普通完成任务
            taskService.complete(taskId, variables);
        }
    }

    // ==================== 加人（Add Approver） ====================

    /**
     * 加人 - 在当前审批节点增加额外的审批人（并行多实例）
     */
    @Transactional
    public void addApprover(String taskId, String newUserId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        User newUser = userRepository.findById(Long.valueOf(newUserId))
                .orElseThrow(() -> new RuntimeException("用户不存在: " + newUserId));

        // 添加候选用户（加签）
        taskService.addUserIdentityLink(taskId, newUser.getName(), IdentityLinkType.CANDIDATE);

        log.info("加人成功: 任务 {} 增加审批人 {}", taskId, newUser.getName());

        String processInstanceId = task.getProcessInstanceId();

        // 发送通知
        sendNotification(newUser.getId(), newUser.getName(),
                "新审批任务", "您被添加为审批人: " + task.getName() + "，请及时处理",
                "TASK_ADD_APPROVER", processInstanceId, taskId);
    }

    // ==================== 私有方法 ====================

    /**
     * 发送回退相关通知
     */
    private void sendBackNotification(String processInstanceId, HistoricTaskInstance targetTask,
                                      String comment, String operationType) {
        String targetAssignee = targetTask.getAssignee();
        if (targetAssignee != null) {
            List<User> users = userRepository.findByName(targetAssignee);
            if (!users.isEmpty()) {
                sendNotification(users.get(0).getId(), targetAssignee,
                        operationType + "通知", "任务被" + operationType + "到您的节点，意见: " + comment,
                        "BACK_" + operationType.toUpperCase(), processInstanceId, null);
            }
        }
    }

    /**
     * 发送通知（使用 Spring Event + 持久化）
     */
    private void sendNotification(Long userId, String username, String title,
                                  String content, String type,
                                  String processInstanceId, String taskId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .username(username)
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

    /**
     * 修改为通过用户ID进行转办 - 直接指定用户ID
     */
    @Transactional
    public void transferTaskByUserId(String taskId, String targetUserName) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        String originalAssignee = task.getAssignee();
        taskService.setAssignee(taskId, targetUserName);

        String processInstanceId = task.getProcessInstanceId();
        runtimeService.setVariable(processInstanceId, "transferFrom_" + taskId, originalAssignee);
        runtimeService.setVariable(processInstanceId, "transferTo_" + taskId, targetUserName);
        runtimeService.setVariable(processInstanceId, "transferTime_" + taskId, new Date());

        log.info("转办成功: 任务 {} 从 {} 转给 {}", taskId, originalAssignee, targetUserName);

        List<User> targetUsers = userRepository.findByName(targetUserName);
        if (!targetUsers.isEmpty()) {
            User targetUser = targetUsers.get(0);
            sendNotification(targetUser.getId(), targetUserName,
                    "任务转办通知", "您收到一个转办任务: " + task.getName(),
                    "TASK_TRANSFER", processInstanceId, taskId);
        }
        if (originalAssignee != null) {
            List<User> originalUsers = userRepository.findByName(originalAssignee);
            if (!originalUsers.isEmpty()) {
                sendNotification(originalUsers.get(0).getId(), originalAssignee,
                        "任务已转办", "您已将任务 " + task.getName() + " 转交给 " + targetUserName,
                        "TASK_TRANSFER", processInstanceId, taskId);
            }
        }
    }
}