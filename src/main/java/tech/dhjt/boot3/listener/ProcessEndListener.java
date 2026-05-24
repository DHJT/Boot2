package tech.dhjt.boot3.listener;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 流程结束监听器 - 流程结束时触发
 */
@Component
public class ProcessEndListener implements ExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ProcessEndListener.class);

    @Override
    public void notify(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String applicantName = (String) execution.getVariable("applicantName");
        String reason = (String) execution.getVariable("reason");
        Boolean approved = (Boolean) execution.getVariable("approved");

        log.info("═══════════════════════════════════════════");
        log.info("【流程结束】流程实例ID: {}", processInstanceId);
        log.info("【流程结束】申请人: {}", applicantName);
        log.info("【流程结束】请假原因: {}", reason);
        log.info("【流程结束】最终审批结果: {}", Boolean.TRUE.equals(approved) ? "✅ 已通过" : "❌ 未通过或无需审批");
        log.info("═══════════════════════════════════════════");
    }
}