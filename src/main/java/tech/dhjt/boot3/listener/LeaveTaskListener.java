package tech.dhjt.boot3.listener;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.flowable.cmmn.model.PlanItemTransition.CREATE;
import static org.flowable.engine.delegate.variable.VariableAggregatorContext.COMPLETE;

/**
 * 请假任务监听器 - 在任务创建/完成时发送通知
 */
@Component
public class LeaveTaskListener implements TaskListener {

    private static final Logger log = LoggerFactory.getLogger(LeaveTaskListener.class);

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskName = delegateTask.getName();
        String processInstanceId = delegateTask.getProcessInstanceId();
        String assignee = delegateTask.getAssignee();

        // 获取流程变量
        String applicantName = (String) delegateTask.getVariable("applicantName");
        String reason = (String) delegateTask.getVariable("reason");
        Integer days = (Integer) delegateTask.getVariable("days");

        if (CREATE.equals(eventName)) {
            // 任务创建通知
//            String candidates = String.join(",", delegateTask.getCandidates());
            String candidates = String.join(",", "132");
            log.info("【审批通知】任务已创建 - 流程实例: {}, 任务: {}, 申请人: {}, 请假原因: {}, 天数: {}",
                    processInstanceId, taskName, applicantName, reason, days);
            log.info("【审批通知】候选组: {}, 待审批人需尽快处理", candidates);

            // 这里可以扩展：发送邮件、短信、WebSocket通知等

        } else if (COMPLETE.equals(eventName)) {
            // 任务完成通知
            Boolean approved = (Boolean) delegateTask.getVariable("approved");
            String comment = (String) delegateTask.getVariable("comment");
            log.info("【审批通知】任务已完成 - 流程实例: {}, 任务: {}, 处理人: {}, 是否同意: {}, 意见: {}",
                    processInstanceId, taskName, assignee, approved, comment);
        }
    }
}