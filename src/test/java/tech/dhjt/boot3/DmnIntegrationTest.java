package tech.dhjt.boot3;

import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tech.dhjt.boot3.service.dmn.DmnService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DMN 决策表集成测试 — 覆盖部署幂等、三张决策表全部规则路径与综合评估一致性
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DMN 决策表集成测试")
class DmnIntegrationTest {

    @Autowired
    private DmnService dmnService;

    @Autowired
    private DmnRepositoryService dmnRepositoryService;

    @Autowired
    private DmnDecisionService dmnDecisionService;

    @BeforeEach
    void deployTables() {
        dmnService.deployAll();
    }

    @Test
    @DisplayName("部署三张决策表且内容指纹幂等（重复部署不产生新版本）")
    void deployAllIsIdempotent() {
        long v1 = dmnRepositoryService.createDecisionQuery().decisionKey("leaveApprovalPath").latestVersion().count();
        long v2 = dmnRepositoryService.createDecisionQuery().decisionKey("leaveDaysDecision").latestVersion().count();
        long v3 = dmnRepositoryService.createDecisionQuery().decisionKey("leaveDepartmentDecision").latestVersion().count();
        assertEquals(1, v1, "leaveApprovalPath 应部署且为最新版本");
        assertEquals(1, v2, "leaveDaysDecision 应部署且为最新版本");
        assertEquals(1, v3, "leaveDepartmentDecision 应部署且为最新版本");

        // 再次部署：内容未变化应跳过，版本不增加
        dmnService.deployAll();
        assertEquals(1, dmnRepositoryService.createDecisionQuery().decisionKey("leaveApprovalPath").latestVersion().count());
        assertEquals(1, dmnRepositoryService.createDecisionQuery().decisionKey("leaveDaysDecision").latestVersion().count());
        assertEquals(1, dmnRepositoryService.createDecisionQuery().decisionKey("leaveDepartmentDecision").latestVersion().count());
    }

    @Test
    @DisplayName("天数决策：<=3 短假，>3 长假")
    void evaluateLeaveDaysRules() {
        assertEquals("short", dmnService.evaluateLeaveDays(2).get("leaveCategory"));
        assertEquals("long", dmnService.evaluateLeaveDays(5).get("leaveCategory"));
        assertEquals("short", dmnService.evaluateLeaveDays(3).get("leaveCategory"));
        assertEquals("long", dmnService.evaluateLeaveDays(4).get("leaveCategory"));
    }

    @Test
    @DisplayName("部门决策：四部门精确匹配 + 默认规则，无重叠错误")
    void evaluateDepartmentRules() {
        assertFalse((Boolean) dmnService.evaluateDepartment("技术部").get("needDeanApproval"));
        assertFalse((Boolean) dmnService.evaluateDepartment("财务部").get("needDeanApproval"));
        assertTrue((Boolean) dmnService.evaluateDepartment("行政部").get("needDeanApproval"));
        assertTrue((Boolean) dmnService.evaluateDepartment("人事部").get("needDeanApproval"));
        // 默认规则：未枚举部门
        assertFalse((Boolean) dmnService.evaluateDepartment("研发部").get("needDeanApproval"));
    }

    @Test
    @DisplayName("审批路径决策：天数+部门全组合（FIRST 策略单规则命中）")
    void evaluateApprovalPathRules() {
        Map<String, Object> r1 = dmnService.evaluateApprovalPath(2, "技术部");
        assertEquals("advisor", r1.get("approvalPath"));
        assertFalse((Boolean) r1.get("finalNeedDeanApproval"));

        Map<String, Object> r2 = dmnService.evaluateApprovalPath(2, "人事部");
        assertEquals("dean", r2.get("approvalPath"));
        assertTrue((Boolean) r2.get("finalNeedDeanApproval"));

        Map<String, Object> r3 = dmnService.evaluateApprovalPath(5, "技术部");
        assertEquals("dean", r3.get("approvalPath"));
        assertTrue((Boolean) r3.get("finalNeedDeanApproval"));

        // 长假优先于部门（规则1）：>3 天即使非敏感部门也走院长
        Map<String, Object> r4 = dmnService.evaluateApprovalPath(5, "研发部");
        assertEquals("dean", r4.get("approvalPath"));
        assertTrue((Boolean) r4.get("finalNeedDeanApproval"));

        // 默认规则：短假 + 未枚举部门
        Map<String, Object> r5 = dmnService.evaluateApprovalPath(2, "研发部");
        assertEquals("advisor", r5.get("approvalPath"));
        assertFalse((Boolean) r5.get("finalNeedDeanApproval"));

        // 边界：恰好 3 天
        Map<String, Object> r6 = dmnService.evaluateApprovalPath(3, "行政部");
        assertEquals("dean", r6.get("approvalPath"));
        assertTrue((Boolean) r6.get("finalNeedDeanApproval"));
    }

    @Test
    @DisplayName("综合评估与审批路径决策结论一致")
    void combinedConsistentWithApprovalPath() {
        for (int days : new int[]{1, 2, 3, 4, 5, 10}) {
            for (String dept : new String[]{"技术部", "财务部", "行政部", "人事部", "研发部"}) {
                boolean combinedNeedDean = (Boolean) dmnService.evaluateCombined(days, dept).get("finalNeedDeanApproval");
                boolean pathNeedDean = (Boolean) dmnService.evaluateApprovalPath(days, dept).get("finalNeedDeanApproval");
                assertEquals(combinedNeedDean, pathNeedDean,
                        String.format("综合评估与审批路径不一致: days=%d, dept=%s", days, dept));
            }
        }
    }
}
