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

import java.util.List;
import java.util.Set;

/**
 * 请假任务监听器 — 在任务创建/完成时发送通知
 * 通过 SpringContextUtil 获取 Bean（因为 Flowable Listener 非 Spring 管理）
 */
@Component("leaveTaskListener")
public class LeaveTaskListener implements TaskListener {

    private static final Logger log = LoggerFactory.getLogger(LeaveTaskListener.class);

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskId = delegateTask.getId();
        String taskName = delegateTask.getName();
        String processInstanceId = delegateTask.getProcessInstanceId();
        String assignee = delegateTask.getAssignee();

        // 获取流程变量
        String applicantName = (String) delegateTask.getVariable("applicantName");
        String reason = (String) delegateTask.getVariable("reason");
        Integer days = (Integer) delegateTask.getVariable("days");
        Object approvalObj = delegateTask.getVariable("approved");
        if (approvalObj == null) {
            approvalObj = delegateTask.getVariable("approval");
        }

        log.info("【审批监听】事件: {} | 任务: {} | 实例: {} | 申请人: {}",
                eventName, taskName, processInstanceId, applicantName);

        try {
            NotificationService notificationService = SpringContextUtil.getBean(NotificationService.class);
            UserService userService = SpringContextUtil.getBean(UserService.class);

            if (EVENTNAME_CREATE.equals(eventName)) {
                // ===== 任务创建通知 =====
                String content = String.format("【新任务】%s - 申请人: %s, 原因: %s, 天数: %d天",
                        taskName, applicantName, reason, days);

                // 1. 发送给具体处理人
                if (assignee != null && !assignee.isEmpty()) {
                    User assigneeUser = userService.getUserByUsername(assignee);
                    if (assigneeUser != null) {
                        notificationService.createNotification(
                                assigneeUser.getId(), assigneeUser.getName(),
                                "新审批任务: " + taskName, content,
                                "TASK_CREATE",
                                processInstanceId, taskId
                        );
                    }
                }

                // 2. 发送给候选组中的所有用户
                Set<IdentityLink> candidates = delegateTask.getCandidates();
                if (candidates != null && !candidates.isEmpty()) {
                    for (IdentityLink link : candidates) {
                        String groupId = link.getGroupId();
                        if (groupId != null && !groupId.isEmpty()) {
                            List<User> groupUsers = userService.getUsersByGroup(groupId);
                            for (User u : groupUsers) {
                                // 如果已经有 assignee 通知了，跳过
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

            } else if (EVENTNAME_COMPLETE.equals(eventName)) {
                // ===== 任务完成通知 =====
                String result = "完成";
                if (approvalObj instanceof Boolean) {
                    result = (Boolean) approvalObj ? "同意" : "拒绝";
                } else if (approvalObj instanceof String) {
                    result = "approved".equals(approvalObj) ? "同意" : "拒绝";
                }

                String comment = (String) delegateTask.getVariable("comment");
                String content = String.format("【审批完成】%s - 处理人: %s, 结果: %s%s",
                        taskName, assignee, result,
                        comment != null ? ", 意见: " + comment : "");

                // 通知申请人
                User applicant = userService.getUserByUsername(applicantName);
                if (applicant != null) {
                    notificationService.createNotification(
                            applicant.getId(), applicant.getName(),
                            "审批结果: " + taskName, content,
                            "TASK_COMPLETE",
                            processInstanceId, taskId
                    );
                }

                log.info("【审批完成】任务已处理: {} | 结果: {} | 申请人: {}",
                        taskName, result, applicantName);
            }
        } catch (Exception e) {
            log.error("发送通知失败: {}", e.getMessage(), e);
        }
    }
}