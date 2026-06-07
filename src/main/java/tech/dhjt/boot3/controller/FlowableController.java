package tech.dhjt.boot3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.dhjt.boot3.model.dto.ProcessDefinitionDTO;
import tech.dhjt.boot3.model.po.Notification;
import tech.dhjt.boot3.model.po.User;
import tech.dhjt.boot3.service.LeaveService;
import tech.dhjt.boot3.service.MultiLevelApprovalProcessService;
import tech.dhjt.boot3.service.NotificationService;
import tech.dhjt.boot3.service.UserService;
import tech.dhjt.boot3.service.impl.ApprovalEnhanceService;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Flowable 工作流 REST API — 完整功能控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/flowable")
@Tag(name = "Flowable 工作流")
public class FlowableController {

    private final LeaveService leaveService;
    private final MultiLevelApprovalProcessService multiLevelApprovalProcessService;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final ApprovalEnhanceService approvalEnhanceService;
    private final UserService userService;
    private final NotificationService notificationService;

    // =====================================================================
    //  通用接口
    // =====================================================================

    /**
     * 查询已部署的流程定义
     */
    @Operation(summary = "查询已部署的流程定义")
    @GetMapping("/definitions")
    public List<ProcessDefinitionDTO> getDefinitions() {
        return repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .orderByProcessDefinitionKey().asc()
                .list()
                .stream()
                .map(ProcessDefinitionDTO::convertToDTO)
                .collect(Collectors.toList());
    }

    // =====================================================================
    //  用户登录 & 用户管理
    // =====================================================================

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username, @RequestParam String password) {
        return userService.login(username, password);
    }

    /**
     * 获取所有用户列表
     */
    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    /**
     * 按用户组查找用户
     */
    @GetMapping("/users/group/{groupId}")
    public List<User> getUsersByGroup(@PathVariable String groupId) {
        return userService.getUsersByGroup(groupId);
    }

    /**
     * 按部门查找用户
     */
    @GetMapping("/users/dept/{deptId}")
    public List<User> getUsersByDept(@PathVariable Long deptId) {
        return userService.getUsersByDept(deptId);
    }

    // =====================================================================
    //  通知接口
    // =====================================================================

    /**
     * 获取用户未读通知
     */
    @GetMapping("/notifications/unread/{userId}")
    public List<Notification> getUnreadNotifications(@PathVariable Long userId) {
        return notificationService.getUnreadNotifications(userId);
    }

    /**
     * 获取用户所有通知
     */
    @GetMapping("/notifications/{userId}")
    public List<Notification> getAllNotifications(@PathVariable Long userId) {
        return notificationService.getAllNotifications(userId);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/notifications/unread/count/{userId}")
    public Map<String, Long> getUnreadCount(@PathVariable Long userId) {
        return Map.of("count", notificationService.getUnreadCount(userId));
    }

    /**
     * 标记通知为已读
     */
    @PostMapping("/notifications/read/{notificationId}")
    public String markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return "已标记为已读";
    }

    /**
     * 标记用户所有通知为已读
     */
    @PostMapping("/notifications/read/all/{userId}")
    public String markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return "全部已标记为已读";
    }

    // =====================================================================
    //  待办任务查询
    // =====================================================================

    /**
     * 查询用户待办任务（按审批人）
     */
    @GetMapping("/tasks/assignee/{assignee}")
    public List<Map<String, Object>> getTasksByAssignee(@PathVariable String assignee) {
        List<Task> tasks = taskService.createTaskQuery()
//                .taskAssignee(assignee)
                .taskCandidateOrAssigned(assignee)
                .orderByTaskCreateTime().desc()
                .list();
//        taskService.createTaskQuery().taskCandidateOrAssigned()

        return tasks.stream().map(task -> {
            Map<String, Object> info = new HashMap<>();
            info.put("taskId", task.getId());
            info.put("taskName", task.getName());
            info.put("processInstanceId", task.getProcessInstanceId());
            info.put("createTime", task.getCreateTime());
            info.put("assignee", task.getAssignee());
            info.put("taskDefinitionKey", task.getTaskDefinitionKey());
            Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
            info.put("applicantName", variables.get("applicantName"));
            info.put("reason", variables.get("reason"));
            info.put("days", variables.get("days"));
            info.put("processKey", runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult()
                    .getProcessDefinitionKey());
            return info;
        }).toList();
    }

    /**
     * 查询用户的候选组待办任务
     */
    @GetMapping("/tasks/candidate/{username}")
    public List<Map<String, Object>> getCandidateTasks(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return List.of();
        }

        List<String> groups = user.getGroupList();
        Set<Map<String, Object>> resultSet = new LinkedHashSet<>();

        for (String group : groups) {
            List<Task> tasks = taskService.createTaskQuery()
                    .taskCandidateGroup(group)
                    .orderByTaskCreateTime().desc()
                    .list();
            for (Task task : tasks) {
                Map<String, Object> info = new HashMap<>();
                info.put("taskId", task.getId());
                info.put("taskName", task.getName());
                info.put("processInstanceId", task.getProcessInstanceId());
                info.put("createTime", task.getCreateTime());
                info.put("candidateGroup", group);
                info.put("assignee", task.getAssignee());
                Map<String, Object> variables = runtimeService.getVariables(task.getProcessInstanceId());
                info.put("applicantName", variables.get("applicantName"));
                info.put("reason", variables.get("reason"));
                info.put("days", variables.get("days"));
                resultSet.add(info);
            }
        }

        return new ArrayList<>(resultSet);
    }

    /**
     * 查询用户的所有待办（个人+候选组）
     */
    @GetMapping("/tasks/all/{username}")
    public List<Map<String, Object>> getAllMyTasks(@PathVariable String username) {
        List<Map<String, Object>> allTasks = new ArrayList<>();
        allTasks.addAll(getTasksByAssignee(username));
        allTasks.addAll(getCandidateTasks(username));
        return allTasks;
    }

    // =====================================================================
    //  请假流程 (leaveProcess) 原接口保留
    // =====================================================================

    @PostMapping("/deploy")
    public String deploy() {
        leaveService.deployProcess();
        return "请假流程已部署";
    }

    @PostMapping("/start")
    public String startLeave(@RequestParam String applicantName,
                             @RequestParam String reason,
                             @RequestParam Integer days) {
        return leaveService.startLeaveProcess(applicantName, reason, days);
    }

    @GetMapping("/tasks/{group}")
    public List<Map<String, Object>> getTasksByGroup(@PathVariable String group) {
        return leaveService.queryTasksByGroup(group);
    }

    @PostMapping("/complete")
    public String completeTask(@RequestParam String taskId,
                               @RequestParam Boolean approved,
                               @RequestParam(required = false) String comment) {
        leaveService.completeTask(taskId, approved, comment);
        return "任务已完成";
    }

    @GetMapping("/all")
    public List<Map<String, Object>> getAllProcesses() {
        return leaveService.queryAllProcesses();
    }

    @GetMapping("/detail/{processInstanceId}")
    public Map<String, Object> getProcessDetail(@PathVariable String processInstanceId) {
        return leaveService.getProcessDetail(processInstanceId);
    }

    @GetMapping(value = "/diagram", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/svg+xml", MediaType.IMAGE_GIF_VALUE})
    public ResponseEntity<byte[]> getDiagram(
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(required = false, defaultValue = "png") String format) {
        try {
            var inputStream = leaveService.getProcessDiagram(processInstanceId, format);
            byte[] imageBytes = inputStream.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(determineMediaType(format))
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // =====================================================================
    //  多级复杂审批流程 (multiLevelApprovalProcess) 原接口保留
    // =====================================================================

    @PostMapping("/multi/deploy")
    public String deployMulti() {
        multiLevelApprovalProcessService.deployProcess();
        return "多级审批流程已部署";
    }

    @PostMapping("/multi/start")
    public String startMulti(@RequestParam String applicantName,
                             @RequestParam String reason,
                             @RequestParam Integer days) {
        return multiLevelApprovalProcessService.startProcess(applicantName, reason, days);
    }

    @GetMapping("/multi/tasks/{group}")
    public List<Map<String, Object>> getMultiTasksByGroup(@PathVariable String group) {
        return multiLevelApprovalProcessService.queryTasksByGroup(group);
    }

    @PostMapping("/multi/complete")
    public String completeMultiTask(@RequestParam String taskId,
                                    @RequestParam String approval,
                                    @RequestParam(required = false) String comment) {
        multiLevelApprovalProcessService.completeTask(taskId, approval, comment);
        return "任务已完成";
    }

    @GetMapping("/multi/all")
    public List<Map<String, Object>> getAllMultiProcesses() {
        return multiLevelApprovalProcessService.queryAllProcesses();
    }

    @GetMapping("/multi/detail/{processInstanceId}")
    public Map<String, Object> getMultiProcessDetail(@PathVariable String processInstanceId) {
        return multiLevelApprovalProcessService.getProcessDetail(processInstanceId);
    }

    @GetMapping(value = "/multi/diagram", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/svg+xml", MediaType.IMAGE_GIF_VALUE})
    public ResponseEntity<byte[]> getMultiDiagram(
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(required = false, defaultValue = "gif") String format,
            @RequestParam(required = false, defaultValue = "completed") String highlightMode) {
        try {
            var inputStream = multiLevelApprovalProcessService.getProcessDiagram(processInstanceId, format, highlightMode);
            byte[] imageBytes = inputStream.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(determineMediaType(format))
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // =====================================================================
    //  审批增强接口 — 逐级回退、驳回重提、转办、委派、加人
    // =====================================================================

    /**
     * 逐级回退 - 回退到上一个审批节点
     */
    @PostMapping("/task/back")
    public String backToPrevious(@RequestParam String taskId,
                                 @RequestParam(required = false, defaultValue = "回退到上一级") String comment) {
        approvalEnhanceService.backToPrevious(taskId, comment);
        return "已回退到上一级审批节点";
    }

    /**
     * 驳回重提 - 驳回回到提交人
     */
    @PostMapping("/task/reject")
    public String rejectToSubmitter(@RequestParam String taskId,
                                    @RequestParam(required = false, defaultValue = "审批不通过，请修改后重新提交") String comment) {
        approvalEnhanceService.rejectToSubmitter(taskId, comment);
        return "已驳回至提交人";
    }

    /**
     * 转办 - 将任务转给指定用户（按用户名）
     */
    @PostMapping("/task/transfer")
    public String transferTask(@RequestParam String taskId,
                               @RequestParam String targetUserName) {
        approvalEnhanceService.transferTaskByUserId(taskId, targetUserName);
        return "任务已转办给: " + targetUserName;
    }

    /**
     * 委派 - 将任务委派给指定用户处理（完成后回到委派人）
     */
    @PostMapping("/task/delegate")
    public String delegateTask(@RequestParam String taskId,
                               @RequestParam String delegateUserId) {
        approvalEnhanceService.delegateTask(taskId, delegateUserId);
        return "任务已委派";
    }

    /**
     * 加人 - 在当前审批节点增加审批人
     */
    @PostMapping("/task/add-approver")
    public String addApprover(@RequestParam String taskId,
                              @RequestParam String newUserId) {
        approvalEnhanceService.addApprover(taskId, newUserId);
        return "已增加审批人";
    }

    /**
     * 完成任务/解决委派任务
     */
    @PostMapping("/task/resolve")
    public String resolveTask(@RequestParam String taskId,
                              @RequestBody(required = false) Map<String, Object> variables) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        approvalEnhanceService.resolveTask(taskId, variables);
        return "任务已完成/已解决";
    }

    /**
     * 获取运行中流程的当前任务列表
     */
    @GetMapping("/process/{processInstanceId}/tasks")
    public List<Map<String, Object>> getProcessTasks(@PathVariable String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();

        return tasks.stream().map(task -> {
            Map<String, Object> info = new HashMap<>();
            info.put("taskId", task.getId());
            info.put("taskName", task.getName());
            info.put("assignee", task.getAssignee());
            info.put("owner", task.getOwner());
            info.put("createTime", task.getCreateTime());
            info.put("taskDefinitionKey", task.getTaskDefinitionKey());

            // 候选人/组
            List<Map<String, String>> candidates = taskService.getIdentityLinksForTask(task.getId()).stream()
                    .map(link -> {
                        Map<String, String> m = new HashMap<>();
                        if (link.getUserId() != null) m.put("userId", link.getUserId());
                        if (link.getGroupId() != null) m.put("groupId", link.getGroupId());
                        return m;
                    })
                    .collect(Collectors.toList());
            info.put("candidates", candidates);
            return info;
        }).toList();
    }

    /**
     * 获取完整的待办统计信息
     */
    @GetMapping("/tasks/stats/{userId}")
    public Map<String, Object> getTaskStats(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            return Map.of("error", "用户不存在");
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("unreadNotifications", notificationService.getUnreadCount(userId));
        stats.put("totalTasks", getAllMyTasks(user.getName()).size());
        return stats;
    }

    // =====================================================================
    //  通用流程图查看接口
    // =====================================================================

    /**
     * 通用流程图查看 - 根据流程定义Key获取流程图（支持多格式）
     *
     * @param processDefinitionKey 流程定义Key（如 leaveProcess、multiLevelApprovalProcess）
     * @param processInstanceId    流程实例ID（可选，传入则高亮当前活动节点）
     * @param format               图片格式：png（默认）、jpg/jpeg、svg、gif
     */
    @GetMapping(value = "/diagram/{processDefinitionKey}", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/svg+xml", MediaType.IMAGE_GIF_VALUE})
    public ResponseEntity<byte[]> getProcessDiagramByKey(
            @PathVariable String processDefinitionKey,
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(required = false, defaultValue = "png") String format,
            @RequestParam(required = false, defaultValue = "completed") String highlightMode) {
        try {
            InputStream inputStream;
            // 根据流程定义Key路由到对应的Service
            if ("leaveProcess".equals(processDefinitionKey)) {
                inputStream = leaveService.getProcessDiagram(processInstanceId, format);
            } else if ("multiLevelApprovalProcess".equals(processDefinitionKey)) {
                inputStream = multiLevelApprovalProcessService.getProcessDiagram(processInstanceId, format, highlightMode);
            } else {
                return ResponseEntity.badRequest().build();
            }
            byte[] imageBytes = inputStream.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(determineMediaType(format))
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据格式字符串确定 MediaType
     */
    private MediaType determineMediaType(String format) {
        if (format == null) {
            return MediaType.IMAGE_PNG;
        }
        return switch (format.toLowerCase()) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "svg" -> MediaType.valueOf("image/svg+xml");
            case "gif" -> MediaType.IMAGE_GIF;
            default -> MediaType.IMAGE_PNG;
        };
    }
}