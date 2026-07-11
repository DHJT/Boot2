package tech.dhjt.boot3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.dhjt.boot3.enums.ProcessKeyEnum;
import tech.dhjt.boot3.service.ProcessService;
import tech.dhjt.boot3.service.flowable.ProcessCommonService;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 统一流程服务实现 — 合并 LeaveServiceImpl 和 MultiLevelApprovalProcessServiceImpl
 *
 * 委托给 ProcessCommonService 执行通用审批操作，本类仅做枚举参数适配和方法代理
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ProcessServiceImpl implements ProcessService {

    private final ProcessCommonService processCommonService;

    /**
     * 部署流程定义
     */
    @Override
    public void deployProcess(ProcessKeyEnum processKey) {
        processCommonService.deployProcess(processKey.getKey(), processKey.getResourcePath(), processKey.getDisplayName());
    }

    /**
     * 启动流程
     */
    @Override
    public String startProcess(ProcessKeyEnum processKey, String applicantName, String reason, Integer days) {
        return startProcess(processKey, applicantName, reason, days, null);
    }

    /**
     * 启动流程（含部门参数，支持 DMN 决策）
     */
    @Override
    public String startProcess(ProcessKeyEnum processKey, String applicantName, String reason, Integer days, String deptName) {
        Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("applicantName", applicantName);
        variables.put("initiator", applicantName);
        variables.put("reason", reason);
        variables.put("days", days);
        if (deptName != null && !deptName.isEmpty()) {
            variables.put("deptName", deptName);
        } else {
            // 从用户信息中获取部门
            try {
                tech.dhjt.boot3.model.po.User user = processCommonService.getUserByUsername(applicantName);
                if (user != null && user.getDeptName() != null) {
                    variables.put("deptName", user.getDeptName());
                } else {
                    variables.put("deptName", "技术部");
                }
            } catch (Exception e) {
                variables.put("deptName", "技术部");
            }
        }
        return processCommonService.startProcessAndSubmit(processKey.getKey(), variables);
    }

    /**
     * 查询用户组待办任务
     */
    @Override
    public List<Map<String, Object>> queryTasksByGroup(String candidateGroup) {
        return processCommonService.queryTasksByGroup(candidateGroup);
    }

    /**
     * 审批任务（兼容 Boolean 和 String 类型审批结果）
     */
    @Override
    public void completeTask(String taskId, Object approved, String comment) {
        processCommonService.approve(taskId, approved, comment);
    }

    /**
     * 查询所有流程列表（运行中+已结束）
     */
    @Override
    public List<Map<String, Object>> queryAllProcesses(ProcessKeyEnum processKey) {
        return processCommonService.queryAllProcesses(processKey.getKey());
    }

    /**
     * 查询流程审批明细
     */
    @Override
    public Map<String, Object> getProcessDetail(String processInstanceId) {
        return processCommonService.getProcessDetail(processInstanceId);
    }

    /**
     * 获取流程图（默认格式 jpg）
     */
    @Override
    public InputStream getProcessDiagram(ProcessKeyEnum processKey, String processInstanceId) {
        return processCommonService.getProcessDiagram(processKey.getKey(), processInstanceId, "jpg", "completed");
    }

    /**
     * 获取流程图（指定格式）
     */
    @Override
    public InputStream getProcessDiagram(ProcessKeyEnum processKey, String processInstanceId, String format) {
        return processCommonService.getProcessDiagram(processKey.getKey(), processInstanceId, format, "completed");
    }

    /**
     * 获取流程图（支持高亮模式）
     */
    @Override
    public InputStream getProcessDiagram(ProcessKeyEnum processKey, String processInstanceId, String format, String highlightMode) {
        return processCommonService.getProcessDiagram(processKey.getKey(), processInstanceId, format, highlightMode);
    }
}