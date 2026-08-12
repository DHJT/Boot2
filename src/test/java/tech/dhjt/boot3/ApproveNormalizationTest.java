package tech.dhjt.boot3;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tech.dhjt.boot3.enums.ProcessKeyEnum;
import tech.dhjt.boot3.service.ProcessService;
import tech.dhjt.boot3.service.dmn.DmnService;
import tech.dhjt.boot3.service.flowable.ProcessCommonService;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 审批归一化 + 通用任务完成端点测试（controller 层 MockMvc）
 * — 覆盖前端真实线格式：query 字符串 approved=true/false 与 /flowable/task/complete 携带表单变量
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("审批归一化与任务完成端点测试")
class ApproveNormalizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProcessService processService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private DmnService dmnService;

    @Autowired
    private ProcessCommonService processCommonService;

    @BeforeEach
    void ensureDmnDeployed() {
        dmnService.deployAll();
    }

    private String startLeave(int days, String deptName, String applicant) {
        return processService.startProcess(ProcessKeyEnum.LEAVE, applicant, "接口测试", days, deptName);
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
    @DisplayName("字符串 approved=false（前端真实线格式）应正确驳回而非静默结束")
    void stringFalseRejectsInsteadOfEnding() throws Exception {
        String pid = startLeave(1, "技术部", "zhangsan");
        Task advisor = findTask(pid, "advisorApproval");
        assertNotNull(advisor);

        mockMvc.perform(post("/flowable/task/approve")
                        .param("taskId", advisor.getId())
                        .param("approved", "false")
                        .param("comment", "驳回"))
                .andExpect(status().isOk());

        // 修复前：approval="false" 被当作未知字符串 → 走默认流直接结束（驳回静默丢失）
        // 修复后：应退回提交人
        assertNotNull(findTask(pid, "submitLeave"), "字符串 false 应触发驳回回路");
        assertFalse(isEnded(pid), "驳回后流程不应结束");
    }

    @Test
    @DisplayName("字符串 approved=true 应正常同意结束")
    void stringTrueApproves() throws Exception {
        String pid = startLeave(1, "技术部", "lisi");
        Task advisor = findTask(pid, "advisorApproval");

        mockMvc.perform(post("/flowable/task/approve")
                        .param("taskId", advisor.getId())
                        .param("approved", "true")
                        .param("comment", "同意"))
                .andExpect(status().isOk());

        assertTrue(isEnded(pid), "字符串 true 应正常结束流程");
    }

    @Test
    @DisplayName("/flowable/task/complete 携带表单变量：重提（改部门为行政部）→ 重新DMN评估走院长")
    void completeTaskWithVariablesResubmits() throws Exception {
        String pid = startLeave(1, "技术部", "zhangsan");

        // 辅导员拒绝（字符串线格式）
        Task advisor = findTask(pid, "advisorApproval");
        mockMvc.perform(post("/flowable/task/approve")
                        .param("taskId", advisor.getId())
                        .param("approved", "false")
                        .param("comment", "请补充材料"))
                .andExpect(status().isOk());

        Task submit = findTask(pid, "submitLeave");
        assertNotNull(submit);

        // 重提：改部门为行政部（短假+行政部 → 需院长审批）
        mockMvc.perform(post("/flowable/task/complete")
                        .param("taskId", submit.getId())
                        .param("applicantName", "zhangsan")
                        .param("reason", "补充材料后重提")
                        .param("days", "2")
                        .param("deptName", "行政部"))
                .andExpect(status().isOk());

        Task dean = findTask(pid, "deanApproval");
        assertNotNull(dean, "行政部短假重提后应走院长审批");
        assertNull(findTask(pid, "advisorApproval"), "行政部不应再走辅导员审批");
    }

    @Test
    @DisplayName("/flowable/task/complete 拒绝覆盖保留变量（approved/approval 不被表单注入）")
    void completeTaskStripsReservedVariables() throws Exception {
        String pid = startLeave(1, "技术部", "zhangsan");

        // 辅导员拒绝
        Task advisor = findTask(pid, "advisorApproval");
        mockMvc.perform(post("/flowable/task/approve")
                        .param("taskId", advisor.getId())
                        .param("approved", "false"))
                .andExpect(status().isOk());

        Task submit = findTask(pid, "submitLeave");

        // 恶意/异常注入保留变量：approved=true 与 finalNeedDeanApproval=false 必须被剥离
        mockMvc.perform(post("/flowable/task/complete")
                        .param("taskId", submit.getId())
                        .param("applicantName", "zhangsan")
                        .param("reason", "注入尝试")
                        .param("days", "5")
                        .param("deptName", "技术部")
                        .param("approved", "true")
                        .param("finalNeedDeanApproval", "false"))
                .andExpect(status().isOk());

        // days=5 → 长假应走院长；若 finalNeedDeanApproval=false 注入成功会错误走辅导员
        assertNotNull(findTask(pid, "deanApproval"), "保留变量注入应被剥离，长假仍走院长审批");
        assertNull(findTask(pid, "advisorApproval"), "保留变量注入应被剥离");
    }

    @Test
    @DisplayName("/flowable/task/complete 拒绝非提交类任务（对审批任务调用应抛异常且任务不被完成）")
    void completeTaskRejectsApprovalTask() {
        String pid = startLeave(1, "技术部", "zhangsan");
        Task advisor = findTask(pid, "advisorApproval");
        assertNotNull(advisor);

        // 对审批任务调用 complete 必须被拒绝（防止绕过审批变量静默走默认流结束）
        assertThrows(IllegalArgumentException.class, () ->
                processCommonService.completeTask(advisor.getId(), java.util.Map.of("days", 1)));

        // 任务未被完成，流程仍停留在审批节点
        assertNotNull(findTask(pid, "advisorApproval"), "审批任务不应被 complete 接口完成");
        assertFalse(isEnded(pid), "流程不应被误结束");
    }
}
