package tech.dhjt.boot3;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多级审批流程集成测试 — 验证 approve 双变量写入对现有 ${approval == 'rejected'} 网关无回归，
 * 并覆盖"驳回→重提→再审批"闭环（该流程的驳回回路为既有能力，本次双写改造不得破坏）。
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("多级审批流程集成测试")
class MultiLevelProcessIntegrationTest {

    @Autowired
    private ProcessService processService;

    @Autowired
    private ProcessCommonService processCommonService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private DmnService dmnService;

    @BeforeEach
    void ensureDmnDeployed() {
        dmnService.deployAll();
    }

    private String startMulti(int days, String applicant) {
        return processService.startProcess(ProcessKeyEnum.MULTI_LEVEL_APPROVAL, applicant, "多级审批测试", days, "技术部");
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
    @DisplayName("短假通过：经理同意 → ≤3天归档(hr) → 同意 → 结束")
    void shortLeaveApprovedEnds() {
        String pid = startMulti(2, "zhangsan");

        Task manager = findTask(pid, "managerApprove");
        assertNotNull(manager, "应产生部门经理审批任务");
        assertEquals(1, taskService.createTaskQuery().taskCandidateGroup("management").processInstanceId(pid).count());

        // String 模式同意（multi 网关依赖 ${approval == 'approved'}）
        processCommonService.approve(manager.getId(), "approved", "同意");
        Task archive = findTask(pid, "archiveTask");
        assertNotNull(archive, "≤3天应直接进入归档节点");
        assertNull(findTask(pid, "directorApprove"), "短假不应产生总监审批");
        assertEquals(1, taskService.createTaskQuery().taskCandidateGroup("hr").processInstanceId(pid).count());

        processCommonService.approve(archive.getId(), "approved", "归档");
        assertTrue(isEnded(pid), "归档后流程应结束");
    }

    @Test
    @DisplayName("长假通过：经理同意 → 总监审批 → 归档 → 结束")
    void longLeaveGoesToDirector() {
        String pid = startMulti(5, "zhangsan");

        Task manager = findTask(pid, "managerApprove");
        processCommonService.approve(manager.getId(), "approved", "同意");
        Task director = findTask(pid, "directorApprove");
        assertNotNull(director, ">3天应产生总监审批");
        assertEquals(1, taskService.createTaskQuery().taskCandidateGroup("directors").processInstanceId(pid).count());

        processCommonService.approve(director.getId(), "approved", "同意");
        Task archive = findTask(pid, "archiveTask");
        assertNotNull(archive, "总监同意后应进入归档");
        processCommonService.approve(archive.getId(), "approved", "归档");
        assertTrue(isEnded(pid));
    }

    @Test
    @DisplayName("驳回回路：经理拒绝 → 退回提交人重提 → 再审批 → 归档 → 结束")
    void rejectThenResubmitThenApprove() {
        String pid = startMulti(2, "lisi");

        // 经理拒绝（String 模式）
        Task manager = findTask(pid, "managerApprove");
        processCommonService.approve(manager.getId(), "rejected", "条件不符");

        // 驳回：退回 submitLeave（assignee=initiator）
        Task submit = findTask(pid, "submitLeave");
        assertNotNull(submit, "拒绝后应退回提交人");
        assertEquals("lisi", submit.getAssignee());

        // 重提（走 /flowable/task/complete 等价服务方法）
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicantName", "lisi");
        variables.put("reason", "修改后重提");
        variables.put("days", 5);
        variables.put("deptName", "技术部");
        processCommonService.completeTask(submit.getId(), variables);

        // 重新进入经理审批
        Task manager2 = findTask(pid, "managerApprove");
        assertNotNull(manager2, "重提后应再次产生经理审批任务");
        processCommonService.approve(manager2.getId(), "approved", "同意");

        // 重提后天数改为 5 → 总监审批
        Task director = findTask(pid, "directorApprove");
        assertNotNull(director, "重提后 >3 天应产生总监审批");
        processCommonService.approve(director.getId(), "approved", "同意");

        Task archive = findTask(pid, "archiveTask");
        processCommonService.approve(archive.getId(), "approved", "归档");
        assertTrue(isEnded(pid), "重提后再审批应正常结束");
    }

    @Test
    @DisplayName("双变量写入回归：Boolean 模式同意也能驱动 multi 的 approval 网关")
    void booleanApproveStillDrivesMultiGateway() {
        String pid = startMulti(2, "wangwu");

        Task manager = findTask(pid, "managerApprove");
        // Boolean 模式同意 → 双写 approval='approved'，multi 网关仍应放行到天数判断
        processCommonService.approve(manager.getId(), true, "同意");
        Task archive = findTask(pid, "archiveTask");
        assertNotNull(archive, "Boolean 同意后应正常进入归档节点（双变量写入保证 approval 网关正确）");
    }
}
