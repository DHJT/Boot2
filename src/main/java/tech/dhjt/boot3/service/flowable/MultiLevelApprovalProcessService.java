package tech.dhjt.boot3.service.flowable;

import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 多级复杂审批流程服务（仿照 LeaveService 实现）
 */
@RequiredArgsConstructor
@Service
public class MultiLevelApprovalProcessService {

    private static final Logger log = LoggerFactory.getLogger(MultiLevelApprovalProcessService.class);

    /** 流程定义Key */
    public static final String PROCESS_KEY = "multiLevelApprovalProcess";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final ProcessEngine processEngine;

    /**
     * 部署流程定义
     */
    public void deployProcess() {
        repositoryService.createDeployment()
                .name("多级复杂审批流程")
                .addClasspathResource("processes/multiLevelApprovalProcess.bpmn20.xml")
                .deploy();
        log.info("多级复杂审批流程已部署");
    }

    /**
     * 启动多级审批流程
     */
    public String startProcess(String applicantName, String reason, Integer days) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicantName", applicantName);
        variables.put("initiator", applicantName);
        variables.put("reason", reason);
        variables.put("days", days);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

        // 自动完成"提交请假申请"任务
        Task submitTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskAssignee(applicantName)
                .singleResult();
        if (submitTask != null) {
            taskService.complete(submitTask.getId());
        }

        log.info("多级审批流程已启动，流程实例ID: {}, 申请人: {}, 天数: {}", processInstance.getId(), applicantName, days);
        return processInstance.getId();
    }

    /**
     * 查询用户组待办任务
     */
    public List<Map<String, Object>> queryTasksByGroup(String candidateGroup) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateGroup(candidateGroup)
                .orderByTaskCreateTime().desc()
                .list();

        return tasks.stream().map(task -> {
            Map<String, Object> info = new HashMap<>();
            info.put("taskId", task.getId());
            info.put("taskName", task.getName());
            info.put("processInstanceId", task.getProcessInstanceId());
            info.put("createTime", task.getCreateTime());
            Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
            info.put("applicantName", variables.get("applicantName"));
            info.put("reason", variables.get("reason"));
            info.put("days", variables.get("days"));
            return info;
        }).toList();
    }

    /**
     * 审批任务（多级审批使用 approval=approved/rejected 字符串）
     * 注意：approval 作为流程变量（用于网关条件判断），同时保存为任务本地变量以便每个审批人独立记录
     */
    public void completeTask(String taskId, String approval, String comment) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("approval", approval);
        variables.put("comment", comment);

        // 先设置任务本地变量，保留每个审批人的独立意见
        taskService.setVariableLocal(taskId, "approval", approval);
        taskService.setVariableLocal(taskId, "comment", comment);

        // complete 时传入的 variables 会变成流程实例变量（用于网关条件判断）
        taskService.complete(taskId, variables);
        log.info("任务 {} 已完成，审批结果: {}, 意见: {}", taskId, approval, comment);
    }

    /**
     * 查询所有流程列表（运行中+已结束），每个流程包含审批时间线（按时间倒序）
     * 运行中的流程排在前面，按最后活跃时间倒序
     */
    public List<Map<String, Object>> queryAllProcesses() {
        List<Map<String, Object>> result = new ArrayList<>();

        // 1. 查询运行中的流程
        List<ProcessInstance> runningList = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .orderByProcessInstanceId().desc()
                .list();

        for (ProcessInstance pi : runningList) {
            Map<String, Object> info = buildProcessInfo(pi.getId(), "running");
            result.add(info);
        }

        // 2. 查询已结束的历史流程
        List<HistoricProcessInstance> historicList = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .finished()
                .orderByProcessInstanceEndTime().desc()
                .list();

        for (HistoricProcessInstance h : historicList) {
            Map<String, Object> info = buildProcessInfo(h.getId(), "finished");
            result.add(info);
        }

        return result;
    }

    /**
     * 构建单个流程的完整信息（含审批时间线）
     */
    private Map<String, Object> buildProcessInfo(String processInstanceId, String status) {
        Map<String, Object> info = new HashMap<>();
        info.put("processInstanceId", processInstanceId);
        info.put("status", status);

        // 获取流程基本信息
        if ("running".equals(status)) {
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (pi != null) {
                info.put("startTime", pi.getStartTime());
                Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
                info.put("applicantName", variables.get("applicantName"));
                info.put("reason", variables.get("reason"));
                info.put("days", variables.get("days"));
            }
        } else {
            HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .includeProcessVariables()
                    .singleResult();
            if (hpi != null) {
                info.put("startTime", hpi.getStartTime());
                info.put("endTime", hpi.getEndTime());
                if (hpi.getProcessVariables() != null) {
                    info.put("applicantName", hpi.getProcessVariables().get("applicantName"));
                    info.put("reason", hpi.getProcessVariables().get("reason"));
                    info.put("days", hpi.getProcessVariables().get("days"));
                }
            }
        }

        // ===== 构建审批时间线（按时间倒序：最新在上面） =====
        List<Map<String, Object>> timeline = new ArrayList<>();

        // 1. 获取所有历史任务实例（已完成的审批节点）
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();

        for (HistoricTaskInstance t : historicTasks) {
            Map<String, Object> node = new HashMap<>();
            node.put("taskId", t.getId());
            node.put("taskName", t.getName());
            node.put("assignee", t.getAssignee());
            node.put("time", t.getEndTime() != null ? t.getEndTime() : t.getCreateTime());
            node.put("status", "completed");

            // 获取审批意见（使用任务本地变量，保留每个审批人的独立意见）
            HistoricTaskInstance completeTask = historyService.createHistoricTaskInstanceQuery()
                    .taskId(t.getId())
                    .includeTaskLocalVariables()
                    .singleResult();
            if (completeTask != null && completeTask.getTaskLocalVariables() != null) {
                // 多级审批使用 approval 为 'approved'/'rejected'
                node.put("approval", completeTask.getTaskLocalVariables().get("approval"));
                node.put("comment", completeTask.getTaskLocalVariables().get("comment"));
            }
            timeline.add(node);
        }

        // 2. 如果是运行中的流程，添加当前待审批节点（在时间线最上面）
        if ("running".equals(status)) {
            List<Task> currentTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .list();

            for (Task task : currentTasks) {
                // 跳过 "提交请假申请" 任务（如果存在）
                if ("提交请假申请".equals(task.getName())) {
                    continue;
                }
                Map<String, Object> node = new HashMap<>();
                node.put("taskId", task.getId());
                node.put("taskName", task.getName());
                node.put("assignee", task.getAssignee());
                node.put("time", task.getCreateTime());
                node.put("status", "pending");
                node.put("approval", null);
                node.put("comment", null);
                // 获取候选组信息
                List<String> candidates = taskService.getIdentityLinksForTask(task.getId()).stream()
                        .map(IdentityLinkInfo::getGroupId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                node.put("candidateGroups", candidates);
                timeline.addFirst(node);
            }
        }

        info.put("timeline", timeline);
        return info;
    }

    /**
     * 获取流程图
     */
    public InputStream getProcessDiagram(String processInstanceId) {
        ProcessDefinition processDefinition;
        BpmnModel bpmnModel;
        List<String> highLightedActivities = new ArrayList<>();
        List<String> highLightedFlows = new ArrayList<>();

        if (processInstanceId != null && !processInstanceId.isEmpty()) {
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            HistoricProcessInstance hpi = null;
            if (pi == null) {
                hpi = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
            }

            if (pi != null) {
                bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
                List<Task> tasks = taskService.createTaskQuery()
                        .processInstanceId(processInstanceId)
                        .list();
                for (Task task : tasks) {
                    highLightedActivities.add(task.getTaskDefinitionKey());
                }
            } else if (hpi != null) {
                bpmnModel = repositoryService.getBpmnModel(hpi.getProcessDefinitionId());
                List<HistoricActivityInstance> historicActivities = historyService
                        .createHistoricActivityInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .orderByHistoricActivityInstanceStartTime().asc()
                        .finished()
                        .list();
                for (HistoricActivityInstance hai : historicActivities) {
                    highLightedActivities.add(hai.getActivityId());
                }
            } else {
                processDefinition = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionKey(PROCESS_KEY)
                        .latestVersion()
                        .singleResult();
                bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            }
        } else {
            processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(PROCESS_KEY)
                    .latestVersion()
                    .singleResult();
            bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        }

        if (bpmnModel == null) {
            throw new RuntimeException("BPMN模型未找到");
        }

        BpmnAutoLayout bpmnAutoLayout = new BpmnAutoLayout(bpmnModel);
        bpmnAutoLayout.setTaskHeight(120);
        bpmnAutoLayout.setTaskWidth(120);
        bpmnAutoLayout.execute();

        ProcessDiagramGenerator generator = new DefaultProcessDiagramGenerator();
        return generator.generateDiagram(
                bpmnModel, "jpg", highLightedActivities,
                highLightedFlows, "宋体", "宋体", "宋体",
                null, 1.0, true);
    }

    /**
     * 查询流程审批明细（带审批时间线，按时间倒序）
     */
    public Map<String, Object> getProcessDetail(String processInstanceId) {
        Map<String, Object> result = new HashMap<>();

        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();

        if (hpi == null) {
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (pi != null) {
                result.put("processInstanceId", pi.getId());
                result.put("processDefinitionId", pi.getProcessDefinitionId());
                result.put("status", "running");
                result.put("startTime", pi.getStartTime());
                Map<String, Object> variables = runtimeService.getVariables(pi.getId());
                result.put("applicantName", variables.get("applicantName"));
                result.put("reason", variables.get("reason"));
                result.put("days", variables.get("days"));
            } else {
                throw new RuntimeException("流程实例不存在: " + processInstanceId);
            }
        } else {
            result.put("processInstanceId", hpi.getId());
            result.put("processDefinitionId", hpi.getProcessDefinitionId());
            result.put("status", "finished");
            result.put("startTime", hpi.getStartTime());
            result.put("endTime", hpi.getEndTime());
            if (hpi.getProcessVariables() != null) {
                result.put("applicantName", hpi.getProcessVariables().get("applicantName"));
                result.put("reason", hpi.getProcessVariables().get("reason"));
                result.put("days", hpi.getProcessVariables().get("days"));
            }
        }

        // 构建审批时间线（按时间倒序）
        List<Map<String, Object>> timeline = new ArrayList<>();

        // 1. 已完成的审批节点（按结束时间倒序）
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();

        for (HistoricTaskInstance t : historicTasks) {
            Map<String, Object> node = new HashMap<>();
            node.put("taskId", t.getId());
            node.put("taskName", t.getName());
            node.put("assignee", t.getAssignee());
            node.put("time", t.getEndTime() != null ? t.getEndTime() : t.getCreateTime());
            node.put("status", "completed");

            HistoricTaskInstance completeTask = historyService.createHistoricTaskInstanceQuery()
                    .taskId(t.getId())
                    .includeTaskLocalVariables()
                    .singleResult();
            if (completeTask != null && completeTask.getTaskLocalVariables() != null) {
                node.put("approval", completeTask.getTaskLocalVariables().get("approval"));
                node.put("comment", completeTask.getTaskLocalVariables().get("comment"));
            } else {
                node.put("approval", null);
                node.put("comment", null);
            }
            timeline.add(node);
        }

        // 2. 运行中的流程添加当前待审批节点（放在最上面）
        String status = (String) result.get("status");
        if ("running".equals(status)) {
            List<Task> currentTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .list();

            for (Task task : currentTasks) {
                if ("提交请假申请".equals(task.getName())) {
                    continue;
                }
                Map<String, Object> node = new HashMap<>();
                node.put("taskId", task.getId());
                node.put("taskName", task.getName());
                node.put("assignee", task.getAssignee());
                node.put("time", task.getCreateTime());
                node.put("status", "pending");
                node.put("approval", null);
                node.put("comment", null);
                List<String> candidates = taskService.getIdentityLinksForTask(task.getId()).stream()
                        .map(IdentityLinkInfo::getGroupId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                node.put("candidateGroups", candidates);
                timeline.addFirst(node);
            }
        }

        result.put("timeline", timeline);
        return result;
    }
}