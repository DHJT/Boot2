package tech.dhjt.boot3;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tech.dhjt.boot3.enums.ProcessKeyEnum;
import tech.dhjt.boot3.service.ProcessService;
import tech.dhjt.boot3.service.dmn.DmnService;
import tech.dhjt.boot3.service.flowable.ProcessCommonService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 请假审批流程集成测试 — 覆盖 DMN 路由、完整审批闭环（同意/拒绝/驳回重提/跨模式变量残留回归）
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("请假审批流程集成测试")
class LeaveProcessIntegrationTest {

    @Autowired
    private ProcessService processService;

    @Autowired
    private ProcessCommonService processCommonService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private DmnService dmnService;

    @BeforeEach
    void ensureDmnDeployed() {
        // 幂等部署，保证 DMN 路由真实生效（测试 profile 已关闭启动自动部署）
        dmnService.deployAll();
    }

    private String startLeave(int days, String deptName, String applicant) {
        return processService.startProcess(ProcessKeyEnum.LEAVE, applicant, "测试请假", days, deptName);
    }

    private Task findTask(String processInstanceId, String taskDefinitionKey) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(taskDefinitionKey)
                .singleResult();
    }

    private boolean isEnded(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .count() == 0;
    }

    @Test
    @DisplayName("场景1：短假（1天·技术部）→ 辅导员审批 → 同意 → 流程结束")
    void shortLeaveGoesToAdvisorAndEnds() {
        String pid = startLeave(1, "技术部", "zhangsan");

        Task advisor = findTask(pid, "advisorApproval");
        assertNotNull(advisor, "短假应产生辅导员审批任务");
        assertNull(findTask(pid, "deanApproval"), "短假不应产生院长审批任务");

        // 候选人查询（candidateUsers=zhangsan,lisi）
        assertEquals(1, taskService.createTaskQuery().taskCandidateUser("zhangsan").processInstanceId(pid).count());
        assertEquals(1, taskService.createTaskQuery().taskCandidateUser("lisi").processInstanceId(pid).count());

        processCommonService.approve(advisor.getId(), true, "同意");
        assertTrue(isEnded(pid), "同意后流程应正常结束");
    }

    @Test
    @DisplayName("场景2：长假（5天·技术部）→ 院长审批（candidateGroups=dean）→ 同意 → 结束")
    void longLeaveGoesToDeanAndEnds() {
        String pid = startLeave(5, "技术部", "zhangsan");

        Task dean = findTask(pid, "deanApproval");
        assertNotNull(dean, "长假应产生院长审批任务");
        assertNull(findTask(pid, "advisorApproval"), "长假不应产生辅导员审批任务");
        assertEquals(1, taskService.createTaskQuery().taskCandidateGroup("dean").processInstanceId(pid).count());

        processCommonService.approve(dean.getId(), true, "同意");
        assertTrue(isEnded(pid));
    }

    @Test
    @DisplayName("场景3：拒绝→驳回重提（改天数5）→ 重新DMN评估走院长 → 同意 → 结束")
    void rejectThenResubmitThenApprove() {
        String pid = startLeave(1, "技术部", "zhangsan");

        // 辅导员拒绝
        Task advisor = findTask(pid, "advisorApproval");
        processCommonService.approve(advisor.getId(), "rejected", "材料不全，驳回");

        // 驳回：任务回到提交人（submitLeave，assignee=申请人）
        Task submit = findTask(pid, "submitLeave");
        assertNotNull(submit, "拒绝后应退回提交人重新提交");
        assertEquals("zhangsan", submit.getAssignee());

        // 重提：修改天数为 5，走 /flowable/task/complete 的等价服务方法
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicantName", "zhangsan");
        variables.put("reason", "重新提交（5天）");
        variables.put("days", 5);
        variables.put("deptName", "技术部");
        processCommonService.completeTask(submit.getId(), variables);

        // DMN 重新评估 → 长假应产生院长任务
        Task dean = findTask(pid, "deanApproval");
        assertNotNull(dean, "重提后重新评估应产生院长审批任务");
        processCommonService.approve(dean.getId(), true, "同意");
        assertTrue(isEnded(pid), "重提后同意应结束流程");
    }

    @Test
    @DisplayName("场景4：变量残留回归——先String拒绝，重提后Boolean同意必须结束（不误驳回）")
    void mixedModeNoStaleVariableBounce() {
        String pid = startLeave(1, "技术部", "zhangsan");

        // 第一轮：String 模式拒绝 → 驳回
        Task advisor1 = findTask(pid, "advisorApproval");
        processCommonService.approve(advisor1.getId(), "rejected", "驳回");
        assertNotNull(findTask(pid, "submitLeave"), "拒绝后应退回提交人");

        // 重提（保持 1 天）
        Task submit = findTask(pid, "submitLeave");
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicantName", "zhangsan");
        variables.put("reason", "再次提交");
        variables.put("days", 1);
        variables.put("deptName", "技术部");
        processCommonService.completeTask(submit.getId(), variables);

        // 第二轮：Boolean 模式同意 —— 双变量写入后不得被残留 approval='rejected' 弹回
        Task advisor2 = findTask(pid, "advisorApproval");
        assertNotNull(advisor2, "重提后应再次产生辅导员审批任务");
        processCommonService.approve(advisor2.getId(), true, "同意");
        assertTrue(isEnded(pid), "跨模式审批后流程必须结束，而非被残留变量误驳回");
    }

    @Test
    @DisplayName("场景5：审批变量双写入（approved + approval + comment）与历史可查询")
    void approvalVariablesWrittenToHistory() {
        String pid = startLeave(2, "财务部", "lisi");

        Task advisor = findTask(pid, "advisorApproval");
        assertNotNull(advisor);
        processCommonService.approve(advisor.getId(), false, "拒绝");

        // 审批结果变量写入（流程变量 + 任务本地变量，历史库各一条）
        List<HistoricVariableInstance> approvedVars = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(pid).variableName("approved").list();
        List<HistoricVariableInstance> approvalVars = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(pid).variableName("approval").list();
        assertFalse(approvedVars.isEmpty(), "应写入 approved 变量");
        assertFalse(approvalVars.isEmpty(), "应写入 approval 变量");
        assertEquals(Boolean.FALSE, approvedVars.getFirst().getValue(), "approved 应为 false");
        assertEquals("rejected", approvalVars.getFirst().getValue(), "approval 应为 rejected");

        // 时间线可查询（驳回后处于提交任务节点）
        Map<String, Object> detail = processCommonService.getProcessDetail(pid);
        assertNotNull(detail);
    }

    @Test
    @DisplayName("场景6：管理能力回归——同意路径时间线含审批节点")
    void timelineBuiltAfterApproval() {
        String pid = startLeave(1, "技术部", "wangwu");
        Task advisor = findTask(pid, "advisorApproval");
        processCommonService.approve(advisor.getId(), true, "同意");

        List<Map<String, Object>> processes = processCommonService.queryAllProcesses("leaveProcess");
        Map<String, Object> match = processes.stream()
                .filter(p -> pid.equals(p.get("processInstanceId")))
                .findFirst().orElse(null);
        assertNotNull(match, "流程应出现在全部流程列表中");
        assertNotNull(match.get("timeline"), "流程详情应包含时间线");
    }
}
