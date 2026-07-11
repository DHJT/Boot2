package tech.dhjt.boot3.service;

import tech.dhjt.boot3.enums.ProcessKeyEnum;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 统一流程服务接口 — 合并 LeaveService 和 MultiLevelApprovalProcessService
 */
public interface ProcessService {

    /**
     * 部署流程定义
     */
    void deployProcess(ProcessKeyEnum processKey);

    /**
     * 启动流程
     */
    String startProcess(ProcessKeyEnum processKey, String applicantName, String reason, Integer days);

    /**
     * 启动流程（含部门参数，支持 DMN 决策）
     */
    String startProcess(ProcessKeyEnum processKey, String applicantName, String reason, Integer days, String deptName);

    /**
     * 查询用户组待办任务
     */
    List<Map<String, Object>> queryTasksByGroup(String group);

    /**
     * 审批任务（兼容 Boolean 和 String 类型审批结果）
     */
    void completeTask(String taskId, Object approved, String comment);

    /**
     * 查询所有流程列表
     */
    List<Map<String, Object>> queryAllProcesses(ProcessKeyEnum processKey);

    /**
     * 查询流程审批明细
     */
    Map<String, Object> getProcessDetail(String processInstanceId);

    /**
     * 获取流程图（默认格式）
     */
    InputStream getProcessDiagram(ProcessKeyEnum processKey, String processInstanceId);

    /**
     * 获取流程图（指定格式）
     */
    InputStream getProcessDiagram(ProcessKeyEnum processKey, String processInstanceId, String format);

    /**
     * 获取流程图（支持高亮模式）
     */
    InputStream getProcessDiagram(ProcessKeyEnum processKey, String processInstanceId, String format, String highlightMode);
}