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

    /**
     * 获取流程图（支持指定格式）
     *
     * @param processInstanceId 流程实例ID（可为空，空时返回最新版本流程图）
     * @param format            图片格式：png、jpg/jpeg、svg、gif 等
     */
    InputStream getProcessDiagram(String processInstanceId, String format);

    /**
     * 获取流程图（支持高亮模式）
     *
     * @param processInstanceId 流程实例ID（可为空，空时返回最新版本流程图）
     * @param format            图片格式：png、jpg/jpeg、svg、gif 等
     * @param highlightMode     高亮模式：all（全部高亮，默认）、completed（仅高亮已走过的节点和连线）
     */
    InputStream getProcessDiagram(String processInstanceId, String format, String highlightMode);
}