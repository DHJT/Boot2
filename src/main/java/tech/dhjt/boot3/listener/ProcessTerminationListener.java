package tech.dhjt.boot3.listener;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.dhjt.boot3.config.SpringContextUtil;
import tech.dhjt.boot3.model.po.User;
import tech.dhjt.boot3.service.NotificationService;
import tech.dhjt.boot3.service.UserService;

/**
 * 流程终止通知监听器 — 当流程直接被终止（非正常结束，如 runtimeService.deleteProcessInstance()）
 * 时，通知提交人（申请人）流程已被终止及相关信息。
 *
 * 该监听器绑定在流程的 end 事件上，通过判断 deleteReason 来区分是正常结束还是被强制终止。
 * 通过 SpringContextUtil 获取 Bean（因为 Flowable Listener 非 Spring 管理）
 */
@Component("processTerminationListener")
public class ProcessTerminationListener implements ExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ProcessTerminationListener.class);

    @Override
    public void notify(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String processDefinitionId = execution.getProcessDefinitionId();

        // 强制转换为它的实现类
        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        // 获取终止原因：正常结束为 null，被终止时会有原因
        String deleteReason = executionEntity.getDeleteReason();

        // 仅在流程被强制终止时有 deleteReason，正常结束不处理
        if (deleteReason == null || deleteReason.isEmpty()) {
            return;
        }

        // 获取流程变量
        String applicantName = (String) execution.getVariable("applicantName");
        String initiator = (String) execution.getVariable("initiator");
        String reason = (String) execution.getVariable("reason");
        Integer days = (Integer) execution.getVariable("days");

        String applicant = applicantName != null ? applicantName : initiator;

        log.info("═══════════════════════════════════════════");
        log.info("【流程终止】流程实例ID: {}", processInstanceId);
        log.info("【流程终止】流程定义: {}", processDefinitionId);
        log.info("【流程终止】申请人: {}", applicant);
        log.info("【流程终止】终止原因: {}", deleteReason);
        log.info("═══════════════════════════════════════════");

        try {
            NotificationService notificationService = SpringContextUtil.getBean(NotificationService.class);
            UserService userService = SpringContextUtil.getBean(UserService.class);

            // 如果申请人或发起人为空，尝试通过历史查询 initiator/startUserId
            if (applicant == null || applicant.isEmpty()) {
                String startUserId = (String) execution.getVariable("startUserId");
                if (startUserId != null && !startUserId.isEmpty()) {
                    applicant = startUserId;
                }
            }

            // 通知申请人/发起人
            if (applicant != null && !applicant.isEmpty()) {
                User submitter = userService.getUserByUsername(applicant);
                if (submitter != null) {
                    String title = "流程已终止";
                    String content = String.format(
                            "【流程终止通知】您的流程申请已被终止。\n" +
                                    "流程实例ID: %s\n" +
                                    "终止原因: %s\n" +
                                    "申请内容: %s\n" +
                                    "天数: %d天",
                            processInstanceId,
                            deleteReason,
                            reason != null ? reason : "无",
                            days != null ? days : 0
                    );

                    notificationService.createNotification(
                            submitter.getId(), submitter.getName(),
                            title, content,
                            "PROCESS_END",
                            processInstanceId, null
                    );

                    log.info("【流程终止】已通知提交人: {} (ID: {})", submitter.getName(), submitter.getId());
                } else {
                    log.warn("【流程终止】未找到提交人用户: {}", applicant);
                }
            } else {
                log.warn("【流程终止】无法确定提交人，跳过通知");
            }

        } catch (Exception e) {
            log.error("【流程终止】发送通知失败: {}", e.getMessage(), e);
        }
    }
}