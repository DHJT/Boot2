package tech.dhjt.boot3.listener;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.dhjt.boot3.config.SpringContextUtil;
import tech.dhjt.boot3.model.po.User;
import tech.dhjt.boot3.service.NotificationService;
import tech.dhjt.boot3.service.UserService;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 用户任务到达通知监听器 — 当用户任务被创建时，
 * 根据设置的办理人(assignee)、候选用户(candidateUsers)、候选组(candidateGroups)、
 * 多实例(multiInstance) 分配的人员列表，通知相应人员。
 *
 * 通过 SpringContextUtil 获取 Bean（因为 Flowable Listener 非 Spring 管理）
 */
@Component("taskNotificationListener")
public class TaskNotificationListener implements TaskListener {

    private static final Logger log = LoggerFactory.getLogger(TaskNotificationListener.class);

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();

        // 仅在任务创建时发送通知
        if (!EVENTNAME_CREATE.equals(eventName)) {
            return;
        }

        String taskId = delegateTask.getId();
        String taskName = delegateTask.getName();
        String processInstanceId = delegateTask.getProcessInstanceId();
        String assignee = delegateTask.getAssignee();

        // 获取流程变量
        String applicantName = (String) delegateTask.getVariable("applicantName");
        String initiator = (String) delegateTask.getVariable("initiator");
        String reason = (String) delegateTask.getVariable("reason");
        Integer days = (Integer) delegateTask.getVariable("days");

        String applicant = applicantName != null ? applicantName : initiator;

        log.info("【任务通知】事件: create | 任务: {} | 实例: {} | 申请人: {}",
                taskName, processInstanceId, applicant);

        try {
            NotificationService notificationService = SpringContextUtil.getBean(NotificationService.class);
            UserService userService = SpringContextUtil.getBean(UserService.class);

            String content = String.format("【新审批任务】%s - 申请人: %s, 原因: %s, 天数: %d天",
                    taskName, applicant, reason != null ? reason : "无", days != null ? days : 0);

            // ===== 1. 发送给具体的办理人 (assignee) =====
            if (assignee != null && !assignee.isEmpty()) {
                notifyAssignee(notificationService, userService, assignee,
                        taskName, content, processInstanceId, taskId);
            }

            // ===== 2. 发送给候选用户 (candidateUsers) =====
            Set<IdentityLink> candidates = delegateTask.getCandidates();
            if (candidates != null && !candidates.isEmpty()) {
                for (IdentityLink link : candidates) {
                    String userId = link.getUserId();
                    if (userId != null && !userId.isEmpty()) {
                        // 如果已经是 assignee，跳过避免重复通知
                        if (assignee != null && assignee.equals(userId)) {
                            continue;
                        }
                        notifyAssignee(notificationService, userService, userId,
                                taskName, content, processInstanceId, taskId);
                    }
                }
            }

            // ===== 3. 发送给候选组中的所有用户 (candidateGroups) =====
            if (candidates != null && !candidates.isEmpty()) {
                for (IdentityLink link : candidates) {
                    String groupId = link.getGroupId();
                    if (groupId != null && !groupId.isEmpty()) {
                        List<User> groupUsers = userService.getUsersByGroup(groupId);
                        for (User u : groupUsers) {
                            // 如果已经是 assignee，跳过避免重复通知
                            if (assignee != null && assignee.equals(u.getUsername())) {
                                continue;
                            }
                            notificationService.createNotification(
                                    u.getId(), u.getName(),
                                    "新审批任务: " + taskName,
                                    content,
                                    "TASK_CREATE",
                                    processInstanceId, taskId
                            );
                        }
                    }
                }
            }

            // ===== 4. 处理多实例任务 (multiInstance) 的人员通知 =====
            // 多实例任务会通过 loopCounter 和 assigneeList 等方式分配人员
            // 检查是否有 assigneeList（多实例人员列表）
            @SuppressWarnings("unchecked")
            Collection<String> assigneeList = (Collection<String>) delegateTask.getVariable("assigneeList");
            if (assigneeList != null && !assigneeList.isEmpty()) {
                for (String multiAssignee : assigneeList) {
                    // 如果已在 assignee 或候选组中通知过，跳过
                    if (assignee != null && assignee.equals(multiAssignee)) {
                        continue;
                    }
                    if (isAlreadyNotifiedInCandidates(candidates, multiAssignee)) {
                        continue;
                    }
                    notifyAssignee(notificationService, userService, multiAssignee,
                            taskName, content, processInstanceId, taskId);
                }
            }

            // 多实例的另一种常见方式：通过用户组 + nrOfInstances/colllection 实现
            @SuppressWarnings("unchecked")
            Collection<String> userList = (Collection<String>) delegateTask.getVariable("userList");
            if (userList != null && !userList.isEmpty()) {
                for (String multiUser : userList) {
                    if (assignee != null && assignee.equals(multiUser)) {
                        continue;
                    }
                    if (isAlreadyNotifiedInCandidates(candidates, multiUser)) {
                        continue;
                    }
                    notifyAssignee(notificationService, userService, multiUser,
                            taskName, content, processInstanceId, taskId);
                }
            }

            // 多实例的用户ID列表（另一种常见命名）
            @SuppressWarnings("unchecked")
            Collection<String> userAssignees = (Collection<String>) delegateTask.getVariable("userAssignees");
            if (userAssignees != null && !userAssignees.isEmpty()) {
                for (String ua : userAssignees) {
                    if (assignee != null && assignee.equals(ua)) {
                        continue;
                    }
                    if (isAlreadyNotifiedInCandidates(candidates, ua)) {
                        continue;
                    }
                    notifyAssignee(notificationService, userService, ua,
                            taskName, content, processInstanceId, taskId);
                }
            }

        } catch (Exception e) {
            log.error("【任务通知】发送通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 通知指定办理人
     */
    private void notifyAssignee(NotificationService notificationService,
                                UserService userService,
                                String assignee,
                                String taskName,
                                String content,
                                String processInstanceId,
                                String taskId) {
        User user = userService.getUserByUsername(assignee);
        if (user != null) {
            notificationService.createNotification(
                    user.getId(), user.getName(),
                    "新审批任务: " + taskName,
                    content,
                    "TASK_CREATE",
                    processInstanceId, taskId
            );
            log.debug("【任务通知】已通知办理人: {} (ID: {})", user.getName(), user.getId());
        } else {
            log.warn("【任务通知】未找到用户: {}", assignee);
        }
    }

    /**
     * 判断候选用户列表中是否已包含指定用户名
     */
    private boolean isAlreadyNotifiedInCandidates(Set<IdentityLink> candidates, String username) {
        if (candidates == null || candidates.isEmpty()) {
            return false;
        }
        return candidates.stream()
                .anyMatch(link -> username.equals(link.getUserId()) || username.equals(link.getGroupId()));
    }
}