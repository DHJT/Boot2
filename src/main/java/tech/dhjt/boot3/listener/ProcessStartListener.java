package tech.dhjt.boot3.listener;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 流程启动监听器 - 流程开始时触发
 */
@Component
public class ProcessStartListener implements ExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ProcessStartListener.class);

    @Override
    public void notify(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String processDefinitionId = execution.getProcessDefinitionId();

        String applicantName = (String) execution.getVariable("applicantName");
        String reason = (String) execution.getVariable("reason");
        Integer days = (Integer) execution.getVariable("days");

        log.info("═══════════════════════════════════════════");
        log.info("【流程启动】流程实例ID: {}", processInstanceId);
        log.info("【流程启动】流程定义: {}", processDefinitionId);
        log.info("【流程启动】申请人: {}", applicantName);
        log.info("【流程启动】请假原因: {}", reason);
        log.info("【流程启动】请假天数: {}天", days);
        log.info("═══════════════════════════════════════════");
    }
}