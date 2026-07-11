package tech.dhjt.boot3.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;
import tech.dhjt.boot3.config.SpringContextUtil;
import tech.dhjt.boot3.model.po.User;
import tech.dhjt.boot3.service.NotificationService;
import tech.dhjt.boot3.service.UserService;

import java.util.List;
import java.util.Map;

import static org.flowable.common.engine.api.delegate.event.FlowableEngineEventType.*;

/**
 * 全局流程事件监听器 — 统一处理所有流程的全局事件
 *
 * 功能：
 * 1. PROCESS_STARTED — 流程启动日志
 * 2. PROCESS_COMPLETED — 流程正常结束日志+通知
 * 3. PROCESS_CANCELLED — 流程被终止时通知提交人，触发申请单数据还原
 * 4. TASK_CREATED — 新任务通知审批人（整合了原 LeaveTaskListener 和 TaskNotificationListener）
 * 5. TASK_COMPLETED — 任务完成通知申请人
 * 6. 超时提醒和终止消息（通过定时+事件联动）
 *
 * 使用 SpringContextUtil 获取 Bean（因为 Flowable Listener 非 Spring 管理）
 */
@Slf4j
@Component("globalProcessEventListener")
public class GlobalProcessEventListener implements FlowableEventListener {

    private static final String SKIP_SUBMITTER_EXPRESSION = "skipSubmitterWhenBackToSubmitter";

    @Override
    public void onEvent(FlowableEvent event) {
        if (!(event instanceof FlowableEngineEntityEvent entityEvent)) {
            return;
        }

        String eventType = event.getType().name();
        log.debug("全局流程事件: type={}", eventType);

        try {
            switch (event.getType()) {
                case PROCESS_STARTED -> handleProcessStarted(entityEvent);
                case PROCESS_COMPLETED -> handleProcessCompleted(entityEvent);
                case PROCESS_CANCELLED -> handleProcessCancelled(entityEvent);
                case TASK_CREATED -> handleTaskCreated(entityEvent);
                case TASK_COMPLETED -> handleTaskCompleted(entityEvent);
                case TASK_ASSIGNED -> handleTaskAssigned(entityEvent);
                default -> { /* 忽略其他事件 */ }
            }
        } catch (Exception e) {
            log.error("全局流程事件处理异常: type={}, error={}", eventType, e.getMessage(), e);
        }
    }

    // =====================================================================
    //  流程启动
    // =====================================================================

    /**
     * 流程启动时记录日志
     */
    private void handleProcessStarted(FlowableEngineEntityEvent event) {
        if (!(event.getEntity() instanceof ExecutionEntity execution)) {
            return;
        }

        String processInstanceId = execution.getProcessInstanceId();
        String processDefinitionId = execution.getProcessDefinitionId();

        // 只处理根执行实例（process instance level）
        if (execution.getParentId() != null) return;

        String applicantName = (String) execution.getVariable("applicantName");
        String reason = (String) execution.getVariable("reason");
        Integer days = (Integer) execution.getVariable("days");

        log.info("═══════════════════════════════════════════");
        log.info("【流程启动】实例ID: {} | 定义: {} | 申请人: {} | 原因: {} | 天数: {}",
                processInstanceId, processDefinitionId, applicantName, reason, days);
        log.info("═══════════════════════════════════════════");
    }

    // =====================================================================
    //  流程正常结束
    // =====================================================================

    /**
     * 流程正常结束时记录日志并通知申请人
     */
    private void handleProcessCompleted(FlowableEngineEntityEvent event) {
        if (!(event.getEntity() instanceof ExecutionEntity execution)) {
            return;
        }
        // 只处理根执行实例
        if (execution.getParentId() != null) return;

        String processInstanceId = execution.getProcessInstanceId();
        String applicantName = (String) execution.getVariable("applicantName");
        String initiator = (String) execution.getVariable("initiator");
        String applicant = applicantName != null ? applicantName : initiator;

        log.info("═══════════════════════════════════════════");
        log.info("【流程正常结束】实例ID: {} | 申请人: {}", processInstanceId, applicant);
        log.info("═══════════════════════════════════════════");

        // 通知申请人
        if (applicant != null) {
            notifyApplicant(applicant, "流程已完成",
                    "您的审批流程已正常结束，流程实例ID: " + processInstanceId,
                    "PROCESS_END", processInstanceId, null);
        }
    }

    // =====================================================================
    //  流程终止（取消）
    // =====================================================================

    /**
     * 流程被终止时 — 通知提交人 + 申请单数据还原（物料领取单）
     *
     * 监听 PROCESS_CANCELLED 事件，通过 deleteReason 判断是否为主动终止
     */
    private void handleProcessCancelled(FlowableEngineEntityEvent event) {
        if (!(event.getEntity() instanceof ExecutionEntity execution)) {
            return;
        }
        // 只处理根执行实例
        if (execution.getParentId() != null) return;

        String processInstanceId = execution.getProcessInstanceId();
        String deleteReason = execution.getDeleteReason();

        // 正常结束 deleteReason 为 null，跳过
        if (deleteReason == null || deleteReason.isEmpty()) {
            return;
        }

        String processDefinitionId = execution.getProcessDefinitionId();
        String applicantName = (String) execution.getVariable("applicantName");
        String initiator = (String) execution.getVariable("initiator");
        String reason = (String) execution.getVariable("reason");
        Integer days = (Integer) execution.getVariable("days");

        String applicant = applicantName != null ? applicantName : initiator;

        log.info("═══════════════════════════════════════════");
        log.info("【流程终止】实例ID: {} | 定义: {} | 申请人: {} | 原因: {}",
                processInstanceId, processDefinitionId, applicant, deleteReason);
        log.info("═══════════════════════════════════════════");

        try {
            // ===== 1. 通知提交人 =====
            if (applicant != null) {
                notifyApplicant(applicant, "流程已终止",
                        String.format("【流程终止通知】您的流程申请已被终止。\n流程实例ID: %s\n终止原因: %s\n申请内容: %s\n天数: %d天",
                                processInstanceId, deleteReason, reason != null ? reason : "无", days != null ? days : 0),
                        "PROCESS_END", processInstanceId, null);
            }

            // ===== 2. 申请单数据还原（物料领取单还原） =====
            restoreOrderData(processInstanceId, execution);

        } catch (Exception e) {
            log.error("流程终止处理异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 申请单数据还原 — 当流程终止时，恢复物料领取单相关数据
     * <p>
     * 具体逻辑由业务方实现，这里提供扩展点：
     * - 从流程变量中获取 orderId / materialOrderId
     * - 调用对应 Service 还原（如将状态从"审批中"恢复为"草稿"或"待提交"）
     */
    private void restoreOrderData(String processInstanceId, ExecutionEntity execution) {
        try {
            // 尝试获取业务相关ID
            Object orderId = execution.getVariable("orderId");
            Object materialOrderId = execution.getVariable("materialOrderId");

            if (orderId != null || materialOrderId != null) {
                log.info("【数据还原】流程终止，准备还原申请单数据: processInstanceId={}, orderId={}, materialOrderId={}",
                        processInstanceId, orderId, materialOrderId);

                // ===== 扩展点：在此处调用业务 Service 还原数据 =====
                // 示例：materialOrderService.restoreToDraft(materialOrderId);
                // 示例：purchaseOrderService.cancelApproval(orderId);

                // 设置流程变量标记已还原
                // runtimeService.setVariable(processInstanceId, "_dataRestored", true);
                // runtimeService.setVariable(processInstanceId, "_dataRestoreTime", new Date());

                log.info("【数据还原】申请单数据还原完成 (扩展实现)");
            } else {
                log.debug("【数据还原】无关联申请单ID，跳过数据还原");
            }
        } catch (Exception e) {
            log.error("【数据还原】申请单数据还原失败: {}", e.getMessage(), e);
        }
    }

    // =====================================================================
    //  任务创建
    // =====================================================================

    /**
     * 任务创建时通知审批人
     * 整合原 LeaveTaskListener 和 TaskNotificationListener 的逻辑
     */
    private void handleTaskCreated(FlowableEngineEntityEvent event) {
        if (!(event.getEntity() instanceof Task task)) {
            return;
        }

        String taskId = task.getId();
        String taskName = task.getName();
        String processInstanceId = task.getProcessInstanceId();
        String assignee = task.getAssignee();

        // 跳过提交申请任务（如果存在）
        if (taskName != null && taskName.contains("提交")) {
            return;
        }

        // 获取流程变量（通过 RuntimeService）
        RuntimeService runtimeService = getBean(RuntimeService.class);
        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
        String applicantName = (String) variables.get("applicantName");
        String initiator = (String) variables.get("initiator");
        String reason = (String) variables.get("reason");
        Integer days = (Integer) variables.get("days");

        String applicant = applicantName != null ? applicantName : initiator;

        log.debug("【任务创建】任务: {} | 实例: {} | 办理人: {} | 申请人: {}",
                taskName, processInstanceId, assignee, applicant);

        try {
            NotificationService notificationService = getBean(NotificationService.class);
            UserService userService = getBean(UserService.class);

            String content = String.format("【新审批任务】%s - 申请人: %s, 原因: %s, 天数: %d天",
                    taskName, applicant, reason != null ? reason : "无", days != null ? days : 0);

            // 1. 通知具体办理人 (assignee)
            if (assignee != null && !assignee.isEmpty()) {
                User assigneeUser = userService.getUserByUsername(assignee);
                if (assigneeUser != null) {
                    notificationService.createNotification(
                            assigneeUser.getId(), assigneeUser.getName(),
                            "新审批任务: " + taskName, content,
                            "TASK_CREATE", processInstanceId, taskId);
                }
            }

            // 2. 从 TaskService 查询并通知所有候选人（包括候选组和候选用户）
            TaskService taskService = getBean(TaskService.class);
            var identityLinks = taskService.getIdentityLinksForTask(taskId);
            java.util.Set<String> notifiedUserIds = new java.util.HashSet<>();
            
            // 如果已经有 assignee 通知了，先记录下来避免重复
            if (assignee != null && !assignee.isEmpty()) {
                notifiedUserIds.add(assignee);
            }

            for (var link : identityLinks) {
                // 2a. 候选组 (candidateGroups)
                if (link.getGroupId() != null) {
                    List<User> groupUsers = userService.getUsersByGroup(link.getGroupId());
                    for (User u : groupUsers) {
                        if (notifiedUserIds.contains(u.getUsername())) {
                            continue; // 避免重复通知
                        }
                        notifiedUserIds.add(u.getUsername());
                        notificationService.createNotification(
                                u.getId(), u.getName(),
                                "新审批任务: " + taskName, content,
                                "TASK_CREATE", processInstanceId, taskId);
                    }
                }
                
                // 2b. 候选用户 (candidateUsers)
                if (link.getUserId() != null && !notifiedUserIds.contains(link.getUserId())) {
                    User candidateUser = userService.getUserByUsername(link.getUserId());
                    if (candidateUser != null) {
                        notifiedUserIds.add(candidateUser.getUsername());
                        notificationService.createNotification(
                                candidateUser.getId(), candidateUser.getName(),
                                "新审批任务: " + taskName, content,
                                "TASK_CREATE", processInstanceId, taskId);
                    }
                }
            }

        } catch (Exception e) {
            log.error("【任务创建通知】失败: {}", e.getMessage(), e);
        }
    }

    // =====================================================================
    //  任务完成
    // =====================================================================

    /**
     * 任务完成时通知申请人
     */
    private void handleTaskCompleted(FlowableEngineEntityEvent event) {
        if (!(event.getEntity() instanceof Task task)) {
            return;
        }

        String taskId = task.getId();
        String taskName = task.getName();
        String processInstanceId = task.getProcessInstanceId();
        String assignee = task.getAssignee();

        // 获取流程变量
        RuntimeService runtimeService = getBean(RuntimeService.class);
        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
        String applicantName = (String) variables.get("applicantName");

        // 获取审批结果（从任务本地变量或流程变量）
        Object approved = runtimeService.getVariable(processInstanceId, "approved");
        if (approved == null) approved = runtimeService.getVariable(processInstanceId, "approval");

        String result;
        if (approved instanceof Boolean) {
            result = Boolean.TRUE.equals(approved) ? "✅ 同意" : "❌ 拒绝";
        } else if ("approved".equals(approved)) {
            result = "✅ 同意";
        } else if ("rejected".equals(approved)) {
            result = "❌ 拒绝";
        } else {
            result = "已完成";
        }

        String comment = (String) runtimeService.getVariable(processInstanceId, "comment");

        log.info("【任务完成】{} | 处理人: {} | 结果: {}", taskName, assignee, result);

        // 通知申请人
        if (applicantName != null) {
            UserService userService = getBean(UserService.class);
            User applicant = userService.getUserByUsername(applicantName);
            if (applicant != null) {
                NotificationService notificationService = getBean(NotificationService.class);
                String content = String.format("【审批完成】%s - 处理人: %s, 结果: %s%s",
                        taskName, assignee, result, comment != null ? ", 意见: " + comment : "");
                notificationService.createNotification(
                        applicant.getId(), applicant.getName(),
                        "审批结果: " + taskName, content,
                        "TASK_COMPLETE", processInstanceId, taskId);
            }
        }
    }

    // =====================================================================
    //  任务分配
    // =====================================================================

    /**
     * 任务分配/认领时通知
     */
    private void handleTaskAssigned(FlowableEngineEntityEvent event) {
        if (!(event.getEntity() instanceof Task task)) {
            return;
        }
        log.debug("【任务分配】taskId={}, assignee={}", task.getId(), task.getAssignee());
        // 可根据需要添加认领通知
    }

    // =====================================================================
    //  工具方法
    // =====================================================================

    /**
     * 通知申请人
     */
    private void notifyApplicant(String applicant, String title, String content,
                                  String type, String processInstanceId, String taskId) {
        try {
            UserService userService = getBean(UserService.class);
            NotificationService notificationService = getBean(NotificationService.class);

            User submitter = userService.getUserByUsername(applicant);
            if (submitter != null) {
                notificationService.createNotification(
                        submitter.getId(), submitter.getName(),
                        title, content, type, processInstanceId, taskId);
            } else {
                log.warn("未找到用户: {}", applicant);
            }
        } catch (Exception e) {
            log.error("通知发送失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从 Spring 容器获取 Bean
     */
    private <T> T getBean(Class<T> clazz) {
        return SpringContextUtil.getBean(clazz);
    }

    @Override
    public boolean isFailOnException() {
        return false; // 不因监听器异常影响流程执行
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}