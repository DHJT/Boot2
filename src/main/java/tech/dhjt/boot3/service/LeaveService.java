package tech.dhjt.boot3.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 请假审批流程服务
 */
public interface LeaveService {

    void deployProcess();

    String startLeaveProcess(String applicantName, String reason, Integer days);

    List<Map<String, Object>> queryTasksByGroup(String group);

    void completeTask(String taskId, Boolean approved, String comment);

    List<Map<String, Object>> queryAllProcesses();

    Map<String, Object> getProcessDetail(String processInstanceId);

    InputStream getProcessDiagram(String processInstanceId);

    /**
     * 获取流程图（支持指定格式）
     *
     * @param processInstanceId 流程实例ID（可为空，空时返回最新版本流程图）
     * @param format            图片格式：png、jpg/jpeg、svg、gif 等
     */
    InputStream getProcessDiagram(String processInstanceId, String format);
}