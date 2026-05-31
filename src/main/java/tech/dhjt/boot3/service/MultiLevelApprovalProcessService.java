package tech.dhjt.boot3.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 多级复杂审批流程服务（仿照 LeaveService 实现）
 */
public interface MultiLevelApprovalProcessService {

    void deployProcess();

    String startProcess(String applicantName, String reason, Integer days);

    List<Map<String, Object>> queryTasksByGroup(String group);

    void completeTask(String taskId, String approval, String comment);

    List<Map<String, Object>> queryAllProcesses();

    Map<String, Object> getProcessDetail(String processInstanceId);

    InputStream getProcessDiagram(String processInstanceId);
}