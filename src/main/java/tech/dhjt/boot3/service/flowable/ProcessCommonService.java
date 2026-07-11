package tech.dhjt.boot3.service.flowable;

import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.dhjt.boot3.model.po.User;
import tech.dhjt.boot3.repository.NotificationRepository;
import tech.dhjt.boot3.service.UserService;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用流程审批服务 — 提供所有流程公用的审批操作：审批、暂停、退回、终止、认领、待办查询等
 * 同时提供流程图生成、审批时间线构建等通用功能
 */
@RequiredArgsConstructor
@Service
public class ProcessCommonService {

    private static final Logger log = LoggerFactory.getLogger(ProcessCommonService.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final UserService userService;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    // =====================================================================
    //  流程部署
    // =====================================================================

    /**
     * 部署流程定义
     *
     * @param processKey 流程定义Key（用于日志）
     * @param resourcePath classpath 资源路径
     * @param deployName  部署名称
     */
    @Transactional
    public void deployProcess(String processKey, String resourcePath, String deployName) {
        repositoryService.createDeployment()
                .name(deployName)
                .addClasspathResource(resourcePath)
                .deploy();
        log.info("流程已部署: key={}, name={}, resource={}", processKey, deployName, resourcePath);
    }

    // =====================================================================
    //  提交 / 启动流程
    // =====================================================================

    /**
     * 启动流程并自动完成提交人任务
     *
     * @param processDefinitionKey 流程定义Key
     * @param variables            流程变量（必须包含：applicantName, initiator, reason, days 等）
     * @return 流程实例ID
     */
    @Transactional
    public String startProcessAndSubmit(String processDefinitionKey, Map<String, Object> variables) {
        // 确保必须有 initiator
        if (!variables.containsKey("initiator") && variables.containsKey("applicantName")) {
            variables.put("initiator", variables.get("applicantName"));
        }
        if (!variables.containsKey("applicantName") && variables.containsKey("initiator")) {
            variables.put("applicantName", variables.get("initiator"));
        }

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);

        // 自动完成提交人第一个任务（"提交请假申请" 或类似任务）
        String applicantName = (String) variables.get("applicantName");
        if (applicantName != null) {
            Task submitTask = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .taskAssignee(applicantName)
                    .singleResult();
            if (submitTask != null) {
                taskService.complete(submitTask.getId());
            }
        }

        log.info("流程已启动: key={}, instanceId={}, applicant={}",
                processDefinitionKey, processInstance.getId(), variables.get("applicantName"));
        return processInstance.getId();
    }

    // =====================================================================
    //  审批
    // =====================================================================

    /**
     * 通用审批 - 通过
     *
     * @param taskId   任务ID
     * @param comment  审批意见
     * @param approved 审批结果（Boolean 或 String "approved"/"rejected"）
     */
    @Transactional
    public void approve(String taskId, Object approved, String comment) {
        Task task = assertTaskExists(taskId);
        String processInstanceId = task.getProcessInstanceId();

        Map<String, Object> variables = new HashMap<>();
        variables.put("comment", comment);

        // 兼容两种模式：Boolean 模式(leave) 和 String 模式(multi)
        if (approved instanceof Boolean) {
            variables.put("approved", approved);
            taskService.setVariableLocal(taskId, "approved", approved);
            taskService.setVariableLocal(taskId, "comment", comment);
        } else if (approved instanceof String approvalStr) {
            variables.put("approval", approvalStr);
            taskService.setVariableLocal(taskId, "approval", approvalStr);
            taskService.setVariableLocal(taskId, "comment", comment);
        } else {
            // 默认按 Boolean 处理
            boolean boolVal = Boolean.parseBoolean(String.valueOf(approved));
            variables.put("approved", boolVal);
            taskService.setVariableLocal(taskId, "approved", boolVal);
            taskService.setVariableLocal(taskId, "comment", comment);
        }

        taskService.complete(taskId, variables);
        log.info("审批完成: taskId={}, approved={}, comment={}", taskId, approved, comment);

        // 记录审批完成通知
        recordOperationLog(processInstanceId, taskId, "APPROVE",
                String.format("审批%s: %s", Boolean.TRUE.equals(approved) || "approved".equals(approved) ? "通过" : "拒绝", comment));
    }

    // =====================================================================
    //  暂停/激活
    // =====================================================================

    /**
     * 暂停流程实例
     */
    @Transactional
    public void suspendProcess(String processInstanceId) {
        runtimeService.suspendProcessInstanceById(processInstanceId);
        log.info("流程已暂停: instanceId={}", processInstanceId);
        recordOperationLog(processInstanceId, null, "SUSPEND", "流程已暂停");
    }

    /**
     * 激活已暂停的流程实例
     */
    @Transactional
    public void activateProcess(String processInstanceId) {
        runtimeService.activateProcessInstanceById(processInstanceId);
        log.info("流程已激活: instanceId={}", processInstanceId);
        recordOperationLog(processInstanceId, null, "ACTIVATE", "流程已恢复");
    }

    // =====================================================================
    //  退回
    // =====================================================================

    /**
     * 退回上一步 — 回退到上一个已完成的不同节点
     */
    @Transactional
    public void backToPrevious(String taskId, String comment) {
        Task currentTask = assertTaskExists(taskId);
        String processInstanceId = currentTask.getProcessInstanceId();
        String currentDefKey = currentTask.getTaskDefinitionKey();

        // 获取已完成的历史任务，按结束时间倒序，找到最近的不同节点
        HistoricTaskInstance previousTask = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list()
                .stream()
                .filter(ht -> !ht.getTaskDefinitionKey().equals(currentDefKey))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("没有找到上一级审批节点，无法回退"));

        doBackToNode(taskId, currentTask, previousTask.getTaskDefinitionKey(), comment, "BACK_TO_PREVIOUS");
    }

    /**
     * 退回至指定节点
     *
     * @param taskId          当前任务ID
     * @param targetNodeKey   目标节点定义Key
     * @param comment         退回意见
     */
    @Transactional
    public void backToNode(String taskId, String targetNodeKey, String comment) {
        Task currentTask = assertTaskExists(taskId);
        doBackToNode(taskId, currentTask, targetNodeKey, comment, "BACK_TO_NODE");
    }

    /**
     * 退回至提交人 — 回退到申请提交节点（第一个用户任务）
     */
    @Transactional
    public void backToSubmitter(String taskId, String comment) {
        Task currentTask = assertTaskExists(taskId);
        String processInstanceId = currentTask.getProcessInstanceId();

        // 找到第一个历史任务的 taskDefinitionKey
        HistoricTaskInstance firstTask = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime().asc()
                .list()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("无法找到提交节点，退回提交人失败"));

        doBackToNode(taskId, currentTask, firstTask.getTaskDefinitionKey(), comment, "BACK_TO_SUBMITTER");
    }

    /**
     * 执行退回操作
     */
    private void doBackToNode(String taskId, Task currentTask, String targetNodeKey, String comment, String operation) {
        String processInstanceId = currentTask.getProcessInstanceId();

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(currentTask.getTaskDefinitionKey(), targetNodeKey)
                .changeState();

        log.info("{} 成功: taskId={}, from={}, to={}, comment={}",
                operation, taskId, currentTask.getTaskDefinitionKey(), targetNodeKey, comment);

        recordOperationLog(processInstanceId, taskId, operation,
                String.format("退回至节点[%s]: %s", targetNodeKey, comment));
    }

    // =====================================================================
    //  终止
    // =====================================================================

    /**
     * 终止流程实例
     *
     * @param processInstanceId 流程实例ID
     * @param reason            终止原因
     */
    @Transactional
    public void terminateProcess(String processInstanceId, String reason) {
        // 先设置终止原因到流程变量，供监听器读取
        runtimeService.setVariable(processInstanceId, "terminationReason", reason);
        runtimeService.setVariable(processInstanceId, "terminationTime", new Date());
        runtimeService.setVariable(processInstanceId, "terminationCandidate", "SYSTEM");

        // 删除流程实例，deleteReason 会触发 ProcessTerminationListener
        runtimeService.deleteProcessInstance(processInstanceId, reason);

        log.info("流程已终止: instanceId={}, reason={}", processInstanceId, reason);
        recordOperationLog(processInstanceId, null, "TERMINATE", "流程终止: " + reason);
    }

    // =====================================================================
    //  任务认领
    // =====================================================================

    /**
     * 认领任务（将候选组任务分配给具体用户）
     */
    @Transactional
    public void claimTask(String taskId, String userId) {
        Task task = assertTaskExists(taskId);
        if (task.getAssignee() != null) {
            throw new RuntimeException("任务已被认领，当前处理人: " + task.getAssignee());
        }
        User user = userService.getUserByUsername(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }
        taskService.claim(taskId, user.getName());
        log.info("任务已认领: taskId={}, user={}", taskId, user.getName());
    }

    /**
     * 取消认领（释放任务回候选组）
     */
    @Transactional
    public void unclaimTask(String taskId) {
        Task task = assertTaskExists(taskId);
        taskService.unclaim(taskId);
        log.info("任务已取消认领: taskId={}", taskId);
    }

    // =====================================================================
    //  待办任务查询
    // =====================================================================

    /**
     * 查询指定用户的个人待办任务（已认领/已分配）
     */
    public List<Map<String, Object>> getPersonalTasks(String assignee) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime().desc()
                .list();
        return buildTaskInfoList(tasks);
    }

    /**
     * 查询用户的候选组待办任务（需要认领）
     */
    public List<Map<String, Object>> getCandidateTasks(String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return List.of();
        }

        Set<String> seenTaskIds = new LinkedHashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();

        // 1. 查询候选用户（candidateUsers）任务 - 直接指派给该用户作为候选人的任务
        try {
            List<Task> userCandidateTasks = taskService.createTaskQuery()
                    .taskCandidateUser(username)
                    .orderByTaskCreateTime().desc()
                    .list();
            for (Task task : userCandidateTasks) {
                if (seenTaskIds.add(task.getId())) {
                    Map<String, Object> info = buildTaskInfo(task);
                    info.put("candidateType", "user");
                    info.put("candidateName", username);
                    info.put("claimable", true);
                    result.add(info);
                }
            }
        } catch (Exception e) {
            log.warn("查询候选用户任务异常: {}", e.getMessage());
        }

        // 2. 查询候选组（candidateGroups）任务
        List<String> groups = user.getGroupList();
        if (!groups.isEmpty()) {
            for (String group : groups) {
                List<Task> tasks = taskService.createTaskQuery()
                        .taskCandidateGroup(group)
                        .orderByTaskCreateTime().desc()
                        .list();
                for (Task task : tasks) {
                    if (seenTaskIds.add(task.getId())) {
                        Map<String, Object> info = buildTaskInfo(task);
                        info.put("candidateType", "group");
                        info.put("candidateGroup", group);
                        info.put("claimable", true);
                        result.add(info);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 查询用户所有的待办（个人已认领 + 候选组待认领 + 候选用户待认领）
     */
    public List<Map<String, Object>> getAllMyTasks(String username) {
        // 个人待办
        List<Map<String, Object>> personal = getPersonalTasks(username);
        personal.forEach(m -> m.put("assignType", "personal"));
        List<Map<String, Object>> all = new ArrayList<>(personal);
        // 候选组待办 + 候选用户待办
        List<Map<String, Object>> candidate = getCandidateTasks(username);
        candidate.forEach(m -> m.put("assignType", "candidate"));
        all.addAll(candidate);
        return all;
    }

    /**
     * 查询用户组待办任务
     */
    public List<Map<String, Object>> queryTasksByGroup(String candidateGroup) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateGroup(candidateGroup)
                .orderByTaskCreateTime().desc()
                .list();
        return buildTaskInfoList(tasks);
    }

    /**
     * 查询用户的候选用户待办任务（直接使用 flowable:candidateUsers 指定的任务）
     */
    public List<Map<String, Object>> getCandidateUserTasks(String username) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateUser(username)
                .orderByTaskCreateTime().desc()
                .list();
        List<Map<String, Object>> result = buildTaskInfoList(tasks);
        for (Map<String, Object> info : result) {
            info.put("candidateType", "user");
            info.put("candidateName", username);
            info.put("claimable", true);
        }
        return result;
    }

    // =====================================================================
    //  流程列表 & 审批时间线
    // =====================================================================

    /**
     * 查询所有流程列表（运行中+已结束）
     */
    public List<Map<String, Object>> queryAllProcesses(String processDefinitionKey) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 运行中流程
        List<ProcessInstance> runningList = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .orderByProcessInstanceId().desc()
                .list();
        for (ProcessInstance pi : runningList) {
            result.add(buildProcessInfo(pi.getId(), "running"));
        }

        // 已结束流程
        List<HistoricProcessInstance> historicList = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .finished()
                .orderByProcessInstanceEndTime().desc()
                .list();
        for (HistoricProcessInstance h : historicList) {
            result.add(buildProcessInfo(h.getId(), "finished"));
        }

        return result;
    }

    /**
     * 查询流程审批明细
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
            if (pi == null) {
                throw new RuntimeException("流程实例不存在: " + processInstanceId);
            }
            fillRunningProcessInfo(result, pi);
        } else {
            fillHistoricProcessInfo(result, hpi);
        }

        // 审批时间线
        result.put("timeline", buildTimeline(processInstanceId, (String) result.get("status")));
        return result;
    }

    // =====================================================================
    //  流程图
    // =====================================================================

    /**
     * 获取流程图（默认格式jpg）
     */
    public InputStream getProcessDiagram(String processDefinitionKey, String processInstanceId) {
        return getProcessDiagram(processDefinitionKey, processInstanceId, "jpg", "completed");
    }

    /**
     * 获取流程图（指定格式）
     */
    public InputStream getProcessDiagram(String processDefinitionKey, String processInstanceId, String format) {
        return getProcessDiagram(processDefinitionKey, processInstanceId, format, "completed");
    }

    /**
     * 获取流程图（支持高亮模式）
     */
    public InputStream getProcessDiagram(String processDefinitionKey, String processInstanceId,
                                          String format, String highlightMode) {
        if (format == null || format.isEmpty()) format = "jpg";
        if ("jpeg".equalsIgnoreCase(format)) format = "jpg";
        if (highlightMode == null || highlightMode.isEmpty()) highlightMode = "completed";

        BpmnModel bpmnModel;
        List<String> highLightedActivities = new ArrayList<>();
        List<String> highLightedFlows = new ArrayList<>();

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

        // 获取bpmnModel
        if (pi != null) {
            bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
        } else if (hpi != null) {
            bpmnModel = repositoryService.getBpmnModel(hpi.getProcessDefinitionId());
        } else {
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .latestVersion()
                    .singleResult();
            if (pd == null) throw new RuntimeException("流程定义未找到: " + processDefinitionKey);
            bpmnModel = repositoryService.getBpmnModel(pd.getId());
        }

        if (bpmnModel == null) throw new RuntimeException("BPMN模型未找到");

        // 高亮模式
        if ("all".equalsIgnoreCase(highlightMode)) {
            collectAllBpmnElements(bpmnModel, highLightedActivities, highLightedFlows);
        } else if (pi != null) {
            // 运行中：高亮当前活动节点
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .list();
            for (Task task : tasks) {
                highLightedActivities.add(task.getTaskDefinitionKey());
            }
        } else if (hpi != null) {
            // 已结束：高亮所有已完成的activity
            List<HistoricActivityInstance> historicActivities = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .finished()
                    .list();
            for (HistoricActivityInstance hai : historicActivities) {
                highLightedActivities.add(hai.getActivityId());
            }
        }

        ProcessDiagramGenerator generator = new DefaultProcessDiagramGenerator();
        return generator.generateDiagram(
                bpmnModel, format, highLightedActivities,
                highLightedFlows, "宋体", "宋体", "宋体",
                null, 1.0, true);
    }

    // =====================================================================
    //  操作记录
    // =====================================================================

    /**
     * 记录审批操作日志（存储在流程变量中，并创建通知）
     */
    public void recordOperationLog(String processInstanceId, String taskId, String operation, String detail) {
        // 存入流程变量
        List<Map<String, Object>> logs;
        try {
            logs = (List<Map<String, Object>>) runtimeService.getVariable(processInstanceId, "_operationLogs");
        } catch (Exception e) {
            logs = null;
        }
        if (logs == null) logs = new ArrayList<>();

        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("operation", operation);
        logEntry.put("detail", detail);
        logEntry.put("taskId", taskId);
        logEntry.put("time", new Date());
        logs.add(logEntry);

        try {
            runtimeService.setVariable(processInstanceId, "_operationLogs", logs);
        } catch (Exception e) {
            log.warn("流程变量记录失败(可能流程已结束): {}", e.getMessage());
        }
    }

    // =====================================================================
    //  简化版审批方法（兼容原接口）
    // =====================================================================

    /**
     * 简化版审批 - Boolean 结果（用于请假流程等）
     */
    @Transactional
    public void completeTask(String taskId, Boolean approved, String comment) {
        approve(taskId, approved, comment);
    }

    /**
     * 简化版审批 - String 结果（用于多级审批流程等）
     */
    @Transactional
    public void completeTask(String taskId, String approval, String comment) {
        approve(taskId, approval, comment);
    }

    // =====================================================================
    //  内部工具方法
    // =====================================================================

    /**
     * 断言任务存在
     */
    private Task assertTaskExists(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new RuntimeException("任务不存在: " + taskId);
        return task;
    }

    /**
     * 构建流程信息
     */
    public Map<String, Object> buildProcessInfo(String processInstanceId, String status) {
        Map<String, Object> info = new HashMap<>();
        info.put("processInstanceId", processInstanceId);
        info.put("status", status);

        if ("running".equals(status)) {
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (pi != null) {
                info.put("startTime", pi.getStartTime());
                putVariables(info, runtimeService.getVariables(processInstanceId));
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
                    putVariables(info, hpi.getProcessVariables());
                }
            }
        }

        info.put("timeline", buildTimeline(processInstanceId, status));
        return info;
    }

    /**
     * 构建审批时间线（按用户任务节点分组，按时间先后排序）
     * <p>
     * 每个节点包含该任务下所有用户的审批记录，对于多实例任务展示所有审批人及意见，
     * 对于待认领任务展示所有候选用户。
     */
    public List<Map<String, Object>> buildTimeline(String processInstanceId, String status) {
        // 1. 按开始时间升序获取所有历史任务实例（包括已完成和运行中）
        List<HistoricTaskInstance> allTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime().asc()
                .list();

        // 2. 如果流程运行中，获取当前活跃任务（用于认领/候选人信息）
        Map<String, Task> runningTaskMap = new HashMap<>();
        if ("running".equals(status)) {
            for (Task t : taskService.createTaskQuery().processInstanceId(processInstanceId).list()) {
                runningTaskMap.put(t.getTaskDefinitionKey(), t);
            }
        }

        // 3. 按 taskDefinitionKey 分组（维持第一次出现的顺序）
        Map<String, List<HistoricTaskInstance>> grouped = new LinkedHashMap<>();
        for (HistoricTaskInstance t : allTasks) {
            grouped.computeIfAbsent(t.getTaskDefinitionKey(), k -> new ArrayList<>()).add(t);
        }

        List<Map<String, Object>> timeline = new ArrayList<>();

        // 4. 遍历每个用户任务节点
        for (Map.Entry<String, List<HistoricTaskInstance>> entry : grouped.entrySet()) {
            String taskDefKey = entry.getKey();
            List<HistoricTaskInstance> taskInstances = entry.getValue();
            HistoricTaskInstance firstTask = taskInstances.getFirst();

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("taskDefinitionKey", taskDefKey);
            node.put("taskName", firstTask.getName());
            node.put("startTime", firstTask.getStartTime());

            // 该任务节点下所有用户的操作记录
            List<Map<String, Object>> userRecords = new ArrayList<>();
            boolean allCompleted = true;

            for (HistoricTaskInstance ht : taskInstances) {
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("assignee", ht.getAssignee());
                record.put("startTime", ht.getStartTime());
                record.put("endTime", ht.getEndTime());

                if (ht.getEndTime() != null) {
                    // 已完成的任务 — 获取审批详情
                    record.put("status", "completed");
                    HistoricTaskInstance detail = historyService.createHistoricTaskInstanceQuery()
                            .taskId(ht.getId())
                            .includeTaskLocalVariables()
                            .singleResult();
                    if (detail != null && detail.getTaskLocalVariables() != null) {
                        Map<String, Object> locals = detail.getTaskLocalVariables();
                        if (locals.containsKey("approved")) record.put("approved", locals.get("approved"));
                        if (locals.containsKey("approval")) record.put("approval", locals.get("approval"));
                        if (locals.containsKey("comment")) record.put("comment", locals.get("comment"));
                    }
                } else {
                    // 运行中的任务（endTime == null）
                    record.put("status", "pending");
                    allCompleted = false;
                }
                userRecords.add(record);
            }

            // 5. 处理当前节点的活跃任务（补充候选人或认领信息）
            Task runningTask = runningTaskMap.get(taskDefKey);
            if (runningTask != null) {
                allCompleted = false;
                // 找到对应的 pending 记录（最后一条 status=pending 的记录）
                boolean updated = false;
                for (Map<String, Object> record : userRecords) {
                    if ("pending".equals(record.get("status"))) {
                        // 更新为当前任务的实时状态
                        record.put("taskId", runningTask.getId());
                        if (runningTask.getAssignee() != null) {
                            // 已认领
                            record.put("assignee", runningTask.getAssignee());
                            record.put("status", "claimed");
                        } else {
                            // 待认领 — 填充候选人信息
                            addCandidateInfo(record, runningTask);
                        }
                        updated = true;
                        break;
                    }
                }
                if (!updated) {
                    // 没有历史 pending 记录（异常情况），新建一条
                    Map<String, Object> newRecord = new LinkedHashMap<>();
                    newRecord.put("assignee", runningTask.getAssignee());
                    newRecord.put("startTime", runningTask.getCreateTime());
                    newRecord.put("endTime", null);
                    newRecord.put("taskId", runningTask.getId());
                    if (runningTask.getAssignee() != null) {
                        newRecord.put("status", "claimed");
                    } else {
                        newRecord.put("status", "pending");
                        addCandidateInfo(newRecord, runningTask);
                    }
                    userRecords.add(newRecord);
                }
            }

            node.put("users", userRecords);
            node.put("status", allCompleted ? "completed" : "pending");

            // 节点结束时间（取已完成记录中最晚的时间）
            taskInstances.stream()
                    .map(HistoricTaskInstance::getEndTime)
                    .filter(Objects::nonNull)
                    .max(Date::compareTo)
                    .ifPresent(endTime -> node.put("endTime", endTime));

            timeline.add(node);
        }

        return timeline;
    }

    /**
     * 向待认领任务记录中添加候选用户和候选组信息
     * <p>
     * 如果有候选组（candidateGroups）但没有直接候选人（candidateUsers），
     * 则从 UserService 解析组内所有用户作为候选人展示。
     */
    private void addCandidateInfo(Map<String, Object> record, Task task) {
        List<IdentityLink> links = taskService.getIdentityLinksForTask(task.getId());
        List<String> candidateUsers = new ArrayList<>();
        List<String> candidateGroups = new ArrayList<>();
        for (IdentityLinkInfo link : links) {
            if (link.getUserId() != null) candidateUsers.add(link.getUserId());
            if (link.getGroupId() != null) candidateGroups.add(link.getGroupId());
        }
        // 如果只有候选组没有直接候选人，解析组内所有用户
        if (candidateUsers.isEmpty() && !candidateGroups.isEmpty()) {
            Set<String> resolvedUsers = new LinkedHashSet<>();
            for (String group : candidateGroups) {
                List<User> users = userService.getUsersByGroup(group);
                for (User u : users) {
                    resolvedUsers.add(u.getName());
                }
            }
            candidateUsers.addAll(resolvedUsers);
        }
        record.put("candidateUsers", candidateUsers);
        record.put("candidateGroups", candidateGroups);
    }

    /**
     * 构建任务信息列表
     */
    private List<Map<String, Object>> buildTaskInfoList(List<Task> tasks) {
        return tasks.stream().map(this::buildTaskInfo).collect(Collectors.toList());
    }

    /**
     * 构建单个任务信息
     */
    public Map<String, Object> buildTaskInfo(Task task) {
        Map<String, Object> info = new HashMap<>();
        info.put("taskId", task.getId());
        info.put("taskName", task.getName());
        info.put("processInstanceId", task.getProcessInstanceId());
        info.put("createTime", task.getCreateTime());
        info.put("assignee", task.getAssignee());
        info.put("taskDefinitionKey", task.getTaskDefinitionKey());
        info.put("owner", task.getOwner());

        // 流程变量
        Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
        info.put("applicantName", variables.get("applicantName"));
        info.put("reason", variables.get("reason"));
        info.put("days", variables.get("days"));

        // 流程定义Key
        try {
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            if (pi != null) {
                info.put("processKey", pi.getProcessDefinitionKey());
            }
        } catch (Exception ignored) {
        }

        return info;
    }

    /**
     * 填充运行中流程信息
     */
    private void fillRunningProcessInfo(Map<String, Object> result, ProcessInstance pi) {
        result.put("processInstanceId", pi.getId());
        result.put("processDefinitionId", pi.getProcessDefinitionId());
        result.put("status", "running");
        result.put("startTime", pi.getStartTime());
        putVariables(result, runtimeService.getVariables(pi.getId()));
    }

    /**
     * 填充历史流程信息
     */
    private void fillHistoricProcessInfo(Map<String, Object> result, HistoricProcessInstance hpi) {
        result.put("processInstanceId", hpi.getId());
        result.put("processDefinitionId", hpi.getProcessDefinitionId());
        result.put("status", "finished");
        result.put("startTime", hpi.getStartTime());
        result.put("endTime", hpi.getEndTime());
        if (hpi.getProcessVariables() != null) {
            putVariables(result, hpi.getProcessVariables());
        }
    }

    /**
     * 将流程变量中的 applicantName/reason/days 放入 result
     */
    private void putVariables(Map<String, Object> result, Map<String, Object> variables) {
        if (variables == null) return;
        if (variables.containsKey("applicantName")) result.put("applicantName", variables.get("applicantName"));
        if (variables.containsKey("reason")) result.put("reason", variables.get("reason"));
        if (variables.containsKey("days")) result.put("days", variables.get("days"));
    }

    /**
     * 收集BPMN模型所有节点和连线（用于 all 高亮模式）
     */
    private void collectAllBpmnElements(BpmnModel bpmnModel,
                                         List<String> activities,
                                         List<String> flows) {
        bpmnModel.getMainProcess().getFlowElements().forEach(fe -> {
            if (fe instanceof org.flowable.bpmn.model.FlowNode) {
                activities.add(fe.getId());
            }
            if (fe instanceof org.flowable.bpmn.model.SequenceFlow) {
                flows.add(fe.getId());
            }
        });
    }
    /**
     * 根据用户名获取用户
     */
    public tech.dhjt.boot3.model.po.User getUserByUsername(String username) {
        return userService.getUserByUsername(username);
    }
}