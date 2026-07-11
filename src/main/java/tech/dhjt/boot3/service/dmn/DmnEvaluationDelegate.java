package tech.dhjt.boot3.service.dmn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * DMN 决策评估委托 — 在 BPMN 流程中通过 serviceTask 调用 DMN 决策
 * <p>
 * 从流程变量中读取 days（请假天数）和 deptName（部门名称），
 * 调用 DMN 决策表进行综合评估，并将结果写回流程变量。
 *
 * @author DHJT
 */
@Slf4j
@RequiredArgsConstructor
@Component("dmnEvaluationDelegate")
public class DmnEvaluationDelegate implements JavaDelegate {

    private final DmnService dmnService;

    @Override
    public void execute(DelegateExecution execution) {
        // 从流程变量中获取输入参数
        Integer days = (Integer) execution.getVariable("days");
        String deptName = (String) execution.getVariable("deptName");

        if (days == null) {
            log.warn("流程变量 days 为空，使用默认值 1");
            days = 1;
        }
        if (deptName == null || deptName.isEmpty()) {
            log.warn("流程变量 deptName 为空，使用默认值 '技术部'");
            deptName = "技术部";
        }

        log.info("执行DMN综合决策评估: days={}, deptName={}", days, deptName);

        // 调用 DMN 服务进行综合评估
        try {
            Map<String, Object> result = dmnService.evaluateCombined(days, deptName);

            // 将决策结果写回流程变量
            String leaveCategory = (String) result.get("leaveCategory");
            Boolean needDeanApproval = (Boolean) result.get("needDeanApproval");
            Boolean finalNeedDean = (Boolean) result.get("finalNeedDeanApproval");

            execution.setVariable("leaveCategory", leaveCategory != null ? leaveCategory : "short");
            execution.setVariable("needDeanApproval", needDeanApproval != null ? needDeanApproval : false);
            execution.setVariable("finalNeedDeanApproval", finalNeedDean != null ? finalNeedDean : false);
            execution.setVariable("dmnDescription", result.getOrDefault("finalDescription",
                    "DMN决策完成"));

            log.info("DMN综合决策结果: leaveCategory={}, needDeanApproval={}, finalNeedDeanApproval={}",
                    leaveCategory, needDeanApproval, finalNeedDean);
        } catch (Exception e) {
            log.error("DMN决策评估执行失败: {}", e.getMessage(), e);
            // 异常时使用安全默认值
            execution.setVariable("leaveCategory", days <= 3 ? "short" : "long");
            execution.setVariable("needDeanApproval", false);
            execution.setVariable("finalNeedDeanApproval", days > 3);
            execution.setVariable("dmnDescription", "DMN决策异常，使用默认规则");
        }
    }
}