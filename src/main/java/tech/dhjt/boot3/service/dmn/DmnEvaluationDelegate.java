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
 * 调用「审批路径决策表 leaveApprovalPath」进行综合评估（天数+部门 → 辅导员/院长），
 * 并将结果写回流程变量（approvalPath、finalNeedDeanApproval），驱动 BPMN 网关路由。
 * 同时保留 leaveCategory/needDeanApproval 等审计变量（来自基础决策表综合评估）。
 * <p>
 * 防御性设计：取变量/类型转换/评估全部置于 try 内，异常时回退安全默认规则
 * （days > 3 需院长审批），保证流程不被 DMN 异常中断。
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
        try {
            // 从流程变量中获取输入参数（防御性类型转换）
            Integer days = resolveDays(execution.getVariable("days"));
            String deptName = resolveDeptName(execution.getVariable("deptName"));

            log.info("执行审批路径DMN决策评估: days={}, deptName={}", days, deptName);

            // 主评估：审批路径决策表（流程路由的单一决策来源）
            Map<String, Object> pathResult = dmnService.evaluateApprovalPath(days, deptName);
            String approvalPath = (String) pathResult.getOrDefault("approvalPath", "advisor");
            boolean finalNeedDean = Boolean.TRUE.equals(pathResult.get("finalNeedDeanApproval"));

            // 审计变量：基础决策表综合评估结果（保留原语义，供时间线/详情展示）
            Map<String, Object> combined = dmnService.evaluateCombined(days, deptName);

            execution.setVariable("approvalPath", approvalPath);
            execution.setVariable("finalNeedDeanApproval", finalNeedDean);
            execution.setVariable("leaveCategory", combined.getOrDefault("leaveCategory", days <= 3 ? "short" : "long"));
            execution.setVariable("needDeanApproval", Boolean.TRUE.equals(combined.get("needDeanApproval")));
            execution.setVariable("dmnDescription", pathResult.getOrDefault("description", "DMN决策完成"));

            log.info("审批路径DMN决策结果: approvalPath={}, finalNeedDeanApproval={}, description={}",
                    approvalPath, finalNeedDean, pathResult.get("description"));
        } catch (Exception e) {
            // 异常时使用安全默认值，保证流程不中断
            Integer days = resolveDays(execution.getVariable("days"));
            log.error("DMN决策评估执行失败，使用默认规则: {}", e.getMessage(), e);
            execution.setVariable("approvalPath", days > 3 ? "dean" : "advisor");
            execution.setVariable("finalNeedDeanApproval", days > 3);
            execution.setVariable("leaveCategory", days <= 3 ? "short" : "long");
            execution.setVariable("needDeanApproval", false);
            execution.setVariable("dmnDescription", "DMN决策异常，使用默认规则");
        }
    }

    /**
     * 解析请假天数（Integer/Number/String 均可，null 时默认 1）
     */
    private Integer resolveDays(Object value) {
        if (value == null) {
            return 1;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value).trim());
    }

    /**
     * 解析部门名称（null/空白时默认"技术部"）
     */
    private String resolveDeptName(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return "技术部";
        }
        return String.valueOf(value).trim();
    }
}
