package tech.dhjt.boot3.service.dmn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.flowable.dmn.api.*;
import org.flowable.dmn.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DMN 决策管理服务 — 提供决策表的部署、评估、查询等统一管理能力
 *
 * @author DHJT
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DmnService {

    private final DmnRepositoryService dmnRepositoryService;
    private final DmnDecisionService dmnDecisionService;

    // =====================================================================
    //  决策表部署
    // =====================================================================

    /**
     * 部署 DMN 决策表（从 classpath 路径加载）
     *
     * @param resourcePath classpath 下的资源路径，如 processes/leaveDaysDecision.dmn
     * @return 部署ID
     */
    @Transactional
    public String deployDecision(String resourcePath) {
        DmnDeploymentBuilder builder = dmnRepositoryService.createDeployment()
                .name("DMN - " + resourcePath)
                .addClasspathResource(resourcePath);
        DmnDeployment deployment = builder.deploy();
        log.info("DMN决策表已部署: resource={}, deploymentId={}", resourcePath, deployment.getId());
        return deployment.getId();
    }

    /**
     * 部署所有预定义的 DMN 决策表
     */
    @Transactional
    public void deployAll() {
        deployDecision("processes/leaveDaysDecision.dmn");
        deployDecision("processes/leaveDepartmentDecision.dmn");
    }

    /**
     * 部署 DMN 文件（通过输入流，支持上传）
     *
     * @param resourceName 资源名称，如 myDecision.dmn
     * @param inputStream  DMN 文件输入流
     * @return 部署ID
     */
    @Transactional
    public String deployDmnFile(String resourceName, InputStream inputStream) {
        DmnDeploymentBuilder builder = dmnRepositoryService.createDeployment()
                .name("DMN - " + resourceName)
                .category("InputStream")
                .addInputStream(resourceName, inputStream);
        DmnDeployment deployment = builder.deploy();
        log.info("DMN决策表已上传部署: resource={}, deploymentId={}", resourceName, deployment.getId());
        return deployment.getId();
    }

    /**
     * 部署 DMN 内容（通过字符串内容）
     *
     * @param resourceName 资源名称，如 myDecision.dmn
     * @param content      DMN 的 XML 字符串内容
     * @return 部署ID
     */
    @Transactional
    public String deployDmnContent(String resourceName, String content) {
        DmnDeploymentBuilder builder = dmnRepositoryService.createDeployment()
                .name("DMN - " + resourceName)
                .addString(resourceName, content);
        DmnDeployment deployment = builder.deploy();
        log.info("DMN决策表已通过内容部署: resource={}, deploymentId={}", resourceName, deployment.getId());
        return deployment.getId();
    }

    // =====================================================================
    //  决策评估
    // =====================================================================

    /**
     * 评估请假天数决策 — 判断请假类别（short/long）
     *
     * @param days 请假天数
     * @return 决策结果，包含 leaveCategory 等字段
     */
    public Map<String, Object> evaluateLeaveDays(int days) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("days", days);

        // 1. 获取Builder并配置
        ExecuteDecisionBuilder builder = dmnDecisionService.createExecuteDecisionBuilder()
                .decisionKey("leaveDaysDecision")
                .variables(variables);

        // 2. 执行决策
        // 场景A: 预期只命中一条规则时，推荐使用此方法
        Map<String, Object> singleResult = dmnDecisionService.executeWithSingleResult(builder);

        // 场景B: 需要审计追踪时，可使用此方法
        // DecisionExecutionAuditContainer auditResult = dmnDecisionService.executeWithAuditTrail(builder);

        // 如果执行的是一个编排了多个决策表的决策服务（Decision Service），应使用专门的方法。
//        Map<String, Object> result = dmnDecisionService.executeDecisionServiceWithSingleResult(builder);

        Map<String, Object> singleResult1 = new HashMap<>();
        singleResult1.put("days", days);

        singleResult1.putAll(singleResult);
        singleResult1.put("leaveCategory", singleResult.get("leaveCategory"));
        singleResult1.put("description", "short".equals(singleResult.get("leaveCategory"))
                    ? "短假（≤3天），由辅导员审批"
                    : "长假（>3天），需院长审批");

//        singleResult1.put("leaveCategory", "short");
//        singleResult1.put("description", "默认短假，由辅导员审批");

        return singleResult1;
    }

    /**
     * 评估部门决策 — 判断是否需要院长审批
     *
     * @param deptName 部门名称
     * @return 决策结果，包含 needDeanApproval 等字段
     */
    public Map<String, Object> evaluateDepartment(String deptName) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("deptName", deptName);

        ExecuteDecisionBuilder builder = dmnDecisionService.createExecuteDecisionBuilder()
                .decisionKey("leaveDepartmentDecision")
                .variables(variables);
        List<Map<String, Object>> result = dmnDecisionService.executeDecision(builder);

        Map<String, Object> singleResult = new HashMap<>();
        singleResult.put("deptName", deptName);
        if (result != null && !result.isEmpty()) {
            Map<String, Object> row = result.getFirst();
            singleResult.putAll(row);
            boolean needDean = Boolean.TRUE.equals(row.get("needDeanApproval"));
            singleResult.put("needDeanApproval", needDean);
            singleResult.put("description", needDean
                    ? deptName + " 需要院长审批"
                    : deptName + " 不需要院长审批");
        } else {
            singleResult.put("needDeanApproval", false);
            singleResult.put("description", deptName + " 按默认规则处理");
        }
        return singleResult;
    }

    /**
     * 综合评估（天数 + 部门） — 用于 BPMN 流程中调用
     *
     * @param days     请假天数
     * @param deptName 部门名称
     * @return 综合决策结果
     */
    public Map<String, Object> evaluateCombined(int days, String deptName) {
        Map<String, Object> daysResult = evaluateLeaveDays(days);
        Map<String, Object> deptResult = evaluateDepartment(deptName);

        Map<String, Object> combined = new HashMap<>();
        combined.putAll(daysResult);
        combined.putAll(deptResult);

        boolean needDean = "long".equals(daysResult.get("leaveCategory"))
                || Boolean.TRUE.equals(deptResult.get("needDeanApproval"));
        combined.put("finalNeedDeanApproval", needDean);
        combined.put("finalDescription", needDean
                ? "需要院长审批（天数较长或部门规则要求）"
                : "由辅导员审批即可");
        return combined;
    }

    // =====================================================================
    //  决策表查询
    // =====================================================================

    /**
     * 查询所有已部署的决策表
     *
     * @return 决策表列表
     */
    public List<Map<String, Object>> listDecisions() {
        List<DmnDecision> decisions = dmnRepositoryService.createDecisionQuery()
                .latestVersion()
                .list();

        return decisions.stream().map(d -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", d.getId());
            info.put("key", d.getKey());
            info.put("name", d.getName());
            info.put("decisionType", d.getDecisionType());
            info.put("diagramResourceName", d.getDiagramResourceName());
            info.put("tenantId", d.getTenantId());
            info.put("category", d.getCategory());
            info.put("version", d.getVersion());
            info.put("deploymentId", d.getDeploymentId());
            info.put("resourceName", d.getResourceName());
            info.put("description", d.getDescription());
            return info;
        }).toList();
    }

    /**
     * 根据决策Key查询决策表详情
     *
     * @param decisionKey 决策Key
     * @return 决策表详情
     */
    public Map<String, Object> getDecisionDetail(String decisionKey) {
        DmnDecision decision = dmnRepositoryService.createDecisionQuery()
                .decisionKey(decisionKey)
                .latestVersion()
                .singleResult();

        if (decision == null) {
            throw new RuntimeException("决策表不存在: " + decisionKey);
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", decision.getId());
        detail.put("key", decision.getKey());
        detail.put("name", decision.getName());
        detail.put("category", decision.getCategory());
        detail.put("version", decision.getVersion());
        detail.put("deploymentId", decision.getDeploymentId());
        detail.put("resourceName", decision.getResourceName());

        try {
            DmnDefinition dmnDefinition = dmnRepositoryService.getDmnDefinition(decision.getId());
            if (dmnDefinition != null) {
                List<Map<String, Object>> decisionsInfo = new ArrayList<>();
                for (Decision dec : dmnDefinition.getDecisions()) {
                    Map<String, Object> decInfo = new HashMap<>();
                    decInfo.put("id", dec.getId());
                    decInfo.put("name", dec.getName());
                    decInfo.put("description", dec.getDescription());

                    if (dec.getExpression() instanceof DecisionTable table) {
                        List<Map<String, String>> inputs = new ArrayList<>();
                        for (InputClause input : table.getInputs()) {
                            Map<String, String> inputInfo = new HashMap<>();
                            if (input.getInputExpression() != null) {
                                inputInfo.put("label", input.getInputExpression().getLabel());
                                inputInfo.put("text", input.getInputExpression().getText() != null
                                        ? input.getInputExpression().getText() : "");
                            }
                            inputs.add(inputInfo);
                        }
                        decInfo.put("inputs", inputs);

                        List<Map<String, String>> outputs = new ArrayList<>();
                        for (OutputClause output : table.getOutputs()) {
                            Map<String, String> outputInfo = new HashMap<>();
                            outputInfo.put("name", output.getName());
                            outputInfo.put("label", output.getLabel());
                            outputInfo.put("typeRef", output.getTypeRef());
                            outputs.add(outputInfo);
                        }
                        decInfo.put("outputs", outputs);

                        decInfo.put("ruleCount", table.getRules() != null ? table.getRules().size() : 0);
                        decInfo.put("hitPolicy", table.getHitPolicy() != null ? table.getHitPolicy().name() : null);
                    }
                    decisionsInfo.add(decInfo);
                }
                detail.put("decisions", decisionsInfo);
            }
        } catch (Exception e) {
            log.warn("解析DMN定义详情异常: {}", e.getMessage());
        }

        return detail;
    }
}