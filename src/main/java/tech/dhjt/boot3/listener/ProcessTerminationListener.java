package tech.dhjt.boot3.listener;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 流程终止监听器 — 仅做日志记录，通知逻辑已由 GlobalProcessEventListener 统一处理
 *
 * 该监听器绑定在流程的 end 事件上，通过判断 deleteReason 来区分是正常结束还是被强制终止。
 * 通知提交人/申请人的逻辑已移至 GlobalProcessEventListener.handleProcessCancelled()
 * 申请单数据还原逻辑也已移至 GlobalProcessEventListener.restoreOrderData()
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
        log.info("【流程终止】实例ID: {} | 定义: {} | 申请人: {} | 原因: {}",
                processInstanceId, processDefinitionId, applicant, deleteReason);
        log.info("【流程终止】申请内容: {} | 天数: {}",
                reason != null ? reason : "无", days != null ? days : 0);
        log.info("【流程终止】(通知与数据还原已由 GlobalProcessEventListener 处理)");
        log.info("═══════════════════════════════════════════");
    }
}