package tech.dhjt.boot3.service.impl;

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
import tech.dhjt.boot3.service.MultiLevelApprovalProcessService;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 多级复杂审批流程服务（仿照 LeaveService 实现）
 */
@RequiredArgsConstructor
@Service
public class MultiLevelApprovalProcessServiceImpl implements MultiLevelApprovalProcessService {

    private static final Logger log = LoggerFactory.getLogger(MultiLevelApprovalProcessServiceImpl.class);

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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
    public InputStream getProcessDiagram(String processInstanceId) {
        return getProcessDiagram(processInstanceId, "jpg");
    }

    /**
     * 获取流程图（支持指定格式）<br>
     * 默认使用 completed 高亮模式，保留向后兼容
     */
    @Override
    public InputStream getProcessDiagram(String processInstanceId, String format) {
        return getProcessDiagram(processInstanceId, format, "completed");
    }

    /**
     * 获取流程图（支持高亮模式）
     *
     * @param processInstanceId 流程实例ID（可为空，空时返回最新版本流程图）
     * @param format            图片格式：png、jpg/jpeg、svg、gif 等
     * @param highlightMode     高亮模式：<br>
     *                          - "all"       : 全部节点和连线高亮<br>
     *                          - "completed" : 仅高亮已走过的节点和连线（默认）
     */
    @Override
    public InputStream getProcessDiagram(String processInstanceId, String format, String highlightMode) {
        if (format == null || format.isEmpty()) {
            format = "jpg";
        }
        // 标准化格式名：将 "jpeg" 统一为 "jpg"
        if ("jpeg".equalsIgnoreCase(format)) {
            format = "jpg";
        }
        if (highlightMode == null || highlightMode.isEmpty()) {
            highlightMode = "completed";
        }

        BpmnModel bpmnModel;
        List<String> highLightedActivities = new ArrayList<>();
        List<String> highLightedFlows = new ArrayList<>();

        // 1. 获取 BpmnModel 和流程实例信息
        ProcessInstance pi = null;
        HistoricProcessInstance hpi = null;

        if (processInstanceId != null && !processInstanceId.isEmpty()) {
            pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (pi == null) {
                hpi = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
            }
        }

        if (pi != null) {
            bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
        } else if (hpi != null) {
            bpmnModel = repositoryService.getBpmnModel(hpi.getProcessDefinitionId());
        } else {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(PROCESS_KEY)
                    .latestVersion()
                    .singleResult();
            bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        }

        if (bpmnModel == null) {
            throw new RuntimeException("BPMN模型未找到");
        }

        // 2. 根据高亮模式收集高亮节点和连线
        if ("all".equalsIgnoreCase(highlightMode)) {
            // ========== 全部高亮模式：所有节点 + 所有连线 ==========
            collectAllElements(bpmnModel, highLightedActivities, highLightedFlows);
        } else {
            // ========== 已走过高亮模式（completed/默认） ==========
            collectCompletedElements(bpmnModel, pi, hpi, highLightedActivities, highLightedFlows);
        }

        // 3. 布局及生成
        BpmnAutoLayout bpmnAutoLayout = new BpmnAutoLayout(bpmnModel);
        bpmnAutoLayout.setTaskHeight(120);
        bpmnAutoLayout.setTaskWidth(120);
        bpmnAutoLayout.execute();

        ProcessDiagramGenerator generator = new DefaultProcessDiagramGenerator();
        return generator.generateDiagram(
                bpmnModel, format, highLightedActivities,
                highLightedFlows, "宋体", "宋体", "宋体",
                null, 1.0, true);
    }

    /**
     * 收集 BPMN 模型中全部节点和连线（all 模式）
     */
    private void collectAllElements(BpmnModel bpmnModel,
                                    List<String> highLightedActivities,
                                    List<String> highLightedFlows) {
        // 所有节点（FlowElement 中所有 activity-like 元素）
        bpmnModel.getMainProcess().getFlowElements().forEach(fe -> {
            if (fe instanceof org.flowable.bpmn.model.UserTask
                    || fe instanceof org.flowable.bpmn.model.ServiceTask
                    || fe instanceof org.flowable.bpmn.model.ScriptTask
                    || fe instanceof org.flowable.bpmn.model.BusinessRuleTask
                    || fe instanceof org.flowable.bpmn.model.ManualTask
                    || fe instanceof org.flowable.bpmn.model.ReceiveTask
                    || fe instanceof org.flowable.bpmn.model.SendTask
                    || fe instanceof org.flowable.bpmn.model.StartEvent
                    || fe instanceof org.flowable.bpmn.model.EndEvent
                    || fe instanceof org.flowable.bpmn.model.ExclusiveGateway
                    || fe instanceof org.flowable.bpmn.model.ParallelGateway
                    || fe instanceof org.flowable.bpmn.model.InclusiveGateway
                    || fe instanceof org.flowable.bpmn.model.BoundaryEvent
                    || fe instanceof org.flowable.bpmn.model.IntermediateCatchEvent
                    || fe instanceof org.flowable.bpmn.model.ThrowEvent) {
                highLightedActivities.add(fe.getId());
            }
        });
        // 所有连线（sequenceFlow）
        bpmnModel.getMainProcess().getFlowElements().forEach(fe -> {
            if (fe instanceof org.flowable.bpmn.model.SequenceFlow) {
                highLightedFlows.add(fe.getId());
            }
        });
    }

    /**
     * 收集已走过的节点和连线（completed 模式）
     */
    private void collectCompletedElements(BpmnModel bpmnModel,
                                          ProcessInstance pi,
                                          HistoricProcessInstance hpi,
                                          List<String> highLightedActivities,
                                          List<String> highLightedFlows) {
        // 获取已完成的 HistoricActivityInstance，按开始时间升序
        String procInstId = (pi != null) ? pi.getId()
                : (hpi != null) ? hpi.getId() : null;

        // 没有流程实例 → 无高亮
        if (procInstId == null) {
            return;
        }

        // 查询流程实例中所有已完成的 Activity（包括开始事件、用户任务、网关、结束事件等）
        List<HistoricActivityInstance> completedActivities = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(procInstId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .finished()
                .list();

        // 收集已完成的节点ID（去重）
        Set<String> completedActivityIds = new LinkedHashSet<>();
        for (HistoricActivityInstance hai : completedActivities) {
            completedActivityIds.add(hai.getActivityId());
        }
        highLightedActivities.addAll(completedActivityIds);

        // 收集已走过的连线：
        // 根据已完成的 Activity 顺序推断出被执行的 sequenceFlow
        // 将 completedActivities 按顺序两两配对，查找 BPMN 模型中连接它们的 sequenceFlow
        if (!completedActivities.isEmpty()) {
            Set<String> traversedFlowIds = new HashSet<>();
            // 先按时间排序（已经按 startTime 升序）
            List<HistoricActivityInstance> sorted = completedActivities;

            for (int i = 0; i < sorted.size() - 1; i++) {
                String fromId = sorted.get(i).getActivityId();
                String toId = sorted.get(i + 1).getActivityId();
                // 查找从 fromId 到 toId 的 sequenceFlow
                findSequenceFlowsBetween(bpmnModel, fromId, toId, traversedFlowIds);
            }

            // 额外处理：开始事件前的入度连线无需处理
            // 额外处理：开始事件到第一个 activity 的连线
            // (已在上述循环中覆盖：startEvent -> firstActivity)

            highLightedFlows.addAll(traversedFlowIds);
        }

        // 如果流程仍在运行中，不额外添加当前待办节点（completed 模式只展示已完成的）
    }

    /**
     * 在 BPMN 模型中查找从 sourceId 到 targetId 的所有 sequenceFlow（支持网关条件连线）
     * 并将找到的连线ID加入 traversedFlows
     */
    private void findSequenceFlowsBetween(BpmnModel bpmnModel, String sourceId, String targetId,
                                           Set<String> traversedFlows) {
        // 查找 BPMN 主流程中所有 SequenceFlow
        bpmnModel.getMainProcess().getFlowElements().forEach(fe -> {
            if (fe instanceof org.flowable.bpmn.model.SequenceFlow) {
                org.flowable.bpmn.model.SequenceFlow sf = (org.flowable.bpmn.model.SequenceFlow) fe;
                if (sourceId.equals(sf.getSourceRef()) && targetId.equals(sf.getTargetRef())) {
                    traversedFlows.add(sf.getId());
                }
            }
        });
    }

    /**
     * 查询流程审批明细（带审批时间线，按时间倒序）
     */
    @Override
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