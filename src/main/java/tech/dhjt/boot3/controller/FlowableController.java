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
import tech.dhjt.boot3.enums.ProcessKeyEnum;
import tech.dhjt.boot3.model.dto.ProcessDefinitionDTO;
import tech.dhjt.boot3.model.po.Notification;
import tech.dhjt.boot3.model.po.User;
import tech.dhjt.boot3.service.NotificationService;
import tech.dhjt.boot3.service.ProcessService;
import tech.dhjt.boot3.service.UserService;
import tech.dhjt.boot3.service.flowable.ApprovalAdminService;
import tech.dhjt.boot3.service.flowable.ProcessCommonService;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Flowable 工作流 REST API — 统一审批接口
 *
 * 提供所有通用的审批操作端点：审批、暂停、退回、终止、认领、待办查询等，
 * 同时提供流程管理接口（改签、加签、去签、中止暂停、终止、强行通过）
 *
 * 所有流程Key参数均使用 ProcessKeyEnum 枚举
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/flowable")
@Tag(name = "Flowable 工作流")
public class FlowableController {

    private final ProcessService processService;
    private final ProcessCommonService processCommonService;
    private final ApprovalAdminService approvalAdminService;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final UserService userService;
    private final NotificationService notificationService;

    // =====================================================================
    //  流程定义
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
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回JWT令牌")
    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username, @RequestParam String password) {
        return userService.login(username, password);
    }

    /**
     * 获取所有用户列表
     */
    @Operation(summary = "获取所有用户列表")
    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    /**
     * 按用户组查找用户
     */
    @Operation(summary = "按用户组查找用户")
    @GetMapping("/users/group/{groupId}")
    public List<User> getUsersByGroup(@PathVariable String groupId) {
        return userService.getUsersByGroup(groupId);
    }

    /**
     * 按部门查找用户
     */
    @Operation(summary = "按部门查找用户")
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
    @Operation(summary = "获取用户未读通知")
    @GetMapping("/notifications/unread/{userId}")
    public List<Notification> getUnreadNotifications(@PathVariable Long userId) {
        return notificationService.getUnreadNotifications(userId);
    }

    /**
     * 获取用户所有通知
     */
    @Operation(summary = "获取用户所有通知")
    @GetMapping("/notifications/{userId}")
    public List<Notification> getAllNotifications(@PathVariable Long userId) {
        return notificationService.getAllNotifications(userId);
    }

    /**
     * 获取未读通知数量
     */
    @Operation(summary = "获取未读通知数量")
    @GetMapping("/notifications/unread/count/{userId}")
    public Map<String, Long> getUnreadCount(@PathVariable Long userId) {
        return Map.of("count", notificationService.getUnreadCount(userId));
    }

    /**
     * 标记通知为已读
     */
    @Operation(summary = "标记通知为已读")
    @PostMapping("/notifications/read/{notificationId}")
    public String markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return "已标记为已读";
    }

    /**
     * 标记用户所有通知为已读
     */
    @Operation(summary = "标记用户所有通知为已读")
    @PostMapping("/notifications/read/all/{userId}")
    public String markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return "全部已标记为已读";
    }

    // =====================================================================
    //  流程枚举管理
    // =====================================================================

    /**
     * 获取所有可用的流程Key枚举
     */
    @Operation(summary = "获取所有可用流程枚举")
    @GetMapping("/process-keys")
    public List<Map<String, String>> getProcessKeys() {
        return Arrays.stream(ProcessKeyEnum.values())
                .map(e -> Map.of(
                        "key", e.getKey(),
                        "displayName", e.getDisplayName()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 校验流程Key是否有效
     */
    @Operation(summary = "校验流程Key")
    @GetMapping("/process-keys/validate")
    public Map<String, Object> validateProcessKey(@RequestParam String key) {
        boolean valid = ProcessKeyEnum.isValid(key);
        return Map.of("valid", valid, "key", key);
    }

    // =====================================================================
    //  统一流程操作（使用枚举）
    // =====================================================================

    /**
     * 部署流程定义（使用枚举）
     */
    @Operation(summary = "部署流程")
    @PostMapping("/deploy")
    public String deploy(@RequestParam String processKey) {
        ProcessKeyEnum key = ProcessKeyEnum.fromKey(processKey);
        processService.deployProcess(key);
        return key.getDisplayName() + "已部署";
    }

    /**
     * 启动流程（使用枚举）
     */
    @Operation(summary = "启动流程")
    @PostMapping("/start")
    public String startProcess(@RequestParam String processKey,
                                @RequestParam String applicantName,
                                @RequestParam String reason,
                                @RequestParam Integer days) {
        ProcessKeyEnum key = ProcessKeyEnum.fromKey(processKey);
        return processService.startProcess(key, applicantName, reason, days);
    }

    /**
     * 启动流程（含部门参数，支持 DMN 决策）
     */
    @Operation(summary = "启动流程（含部门）", description = "支持 DMN 决策评估的流程启动接口，需要传入部门名称")
    @PostMapping("/start-with-dept")
    public String startProcessWithDept(@RequestParam String processKey,
                                        @RequestParam String applicantName,
                                        @RequestParam String reason,
                                        @RequestParam Integer days,
                                        @RequestParam(defaultValue = "技术部") String deptName) {
        ProcessKeyEnum key = ProcessKeyEnum.fromKey(processKey);
        return processService.startProcess(key, applicantName, reason, days, deptName);
    }

    /**
     * 查询用户组待办任务
     */
    @Operation(summary = "查询用户组待办任务")
    @GetMapping("/tasks/group/{group}")
    public List<Map<String, Object>> getTasksByGroup(@PathVariable String group) {
        return processService.queryTasksByGroup(group);
    }

    /**
     * 查询用户的所有待办（个人+候选组+候选用户）
     */
    @Operation(summary = "查询用户所有待办（个人+候选组+候选用户）")
    @GetMapping("/tasks/all/{username}")
    public List<Map<String, Object>> getAllMyTasks(@PathVariable String username) {
        return processCommonService.getAllMyTasks(username);
    }

    /**
     * 查询用户的个人待办任务（已分配/已认领）
     */
    @Operation(summary = "查询用户个人待办（已分配/已认领）")
    @GetMapping("/tasks/personal/{username}")
    public List<Map<String, Object>> getPersonalTasks(@PathVariable String username) {
        return processCommonService.getPersonalTasks(username);
    }

    /**
     * 查询用户的候选组待办任务（需要认领）
     */
    @Operation(summary = "查询用户候选组待办（需认领）")
    @GetMapping("/tasks/candidate/{username}")
    public List<Map<String, Object>> getCandidateTasks(@PathVariable String username) {
        return processCommonService.getCandidateTasks(username);
    }

    /**
     * 查询用户的候选用户待办任务（使用 flowable:candidateUsers 指定）
     */
    @Operation(summary = "查询用户候选用户待办（candidateUsers 任务）")
    @GetMapping("/tasks/candidate-user/{username}")
    public List<Map<String, Object>> getCandidateUserTasks(@PathVariable String username) {
        return processCommonService.getCandidateUserTasks(username);
    }

    /**
     * 获取运行中流程的当前任务列表
     */
    @Operation(summary = "获取流程当前任务列表")
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
    @Operation(summary = "获取待办统计信息")
    @GetMapping("/tasks/stats/{userId}")
    public Map<String, Object> getTaskStats(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        if (user == null) return Map.of("error", "用户不存在");

        Map<String, Object> stats = new HashMap<>();
        stats.put("unreadNotifications", notificationService.getUnreadCount(userId));
        stats.put("personalTasks", processCommonService.getPersonalTasks(user.getName()).size());
        stats.put("candidateTasks", processCommonService.getCandidateTasks(user.getName()).size());
        return stats;
    }

    // =====================================================================
    //  统一审批操作（通用接口）
    // =====================================================================

    /**
     * 通用审批 — 通过/拒绝
     */
    @Operation(summary = "通用审批（通过/拒绝）")
    @PostMapping("/task/approve")
    public String approveTask(@RequestParam String taskId,
                               @RequestParam Object approved,
                               @RequestParam(required = false, defaultValue = "") String comment) {
        processCommonService.approve(taskId, approved, comment);
        return "审批完成";
    }

    /**
     * 暂停流程
     */
    @Operation(summary = "暂停流程")
    @PostMapping("/process/suspend")
    public String suspendProcess(@RequestParam String processInstanceId,
                                  @RequestParam(required = false, defaultValue = "管理员暂停") String reason) {
        processCommonService.suspendProcess(processInstanceId);
        return "流程已暂停";
    }

    /**
     * 激活流程（恢复暂停）
     */
    @Operation(summary = "激活流程（恢复暂停）")
    @PostMapping("/process/activate")
    public String activateProcess(@RequestParam String processInstanceId) {
        processCommonService.activateProcess(processInstanceId);
        return "流程已恢复";
    }

    /**
     * 退回上一步
     */
    @Operation(summary = "退回上一步")
    @PostMapping("/task/back")
    public String backToPrevious(@RequestParam String taskId,
                                  @RequestParam(required = false, defaultValue = "回退到上一级") String comment) {
        processCommonService.backToPrevious(taskId, comment);
        return "已回退到上一级审批节点";
    }

    /**
     * 退回至指定节点
     */
    @Operation(summary = "退回至指定节点")
    @PostMapping("/task/back-to-node")
    public String backToNode(@RequestParam String taskId,
                              @RequestParam String targetNodeKey,
                              @RequestParam(required = false, defaultValue = "退回至指定节点") String comment) {
        processCommonService.backToNode(taskId, targetNodeKey, comment);
        return "已退回至指定节点: " + targetNodeKey;
    }

    /**
     * 退回至提交人
     */
    @Operation(summary = "退回至提交人")
    @PostMapping("/task/back-to-submitter")
    public String backToSubmitter(@RequestParam String taskId,
                                   @RequestParam(required = false, defaultValue = "审批不通过，请修改后重新提交") String comment) {
        processCommonService.backToSubmitter(taskId, comment);
        return "已驳回至提交人";
    }

    /**
     * 终止流程
     */
    @Operation(summary = "终止流程")
    @PostMapping("/process/terminate")
    public String terminateProcess(@RequestParam String processInstanceId,
                                    @RequestParam String reason) {
        processCommonService.terminateProcess(processInstanceId, reason);
        return "流程已终止";
    }

    /**
     * 认领任务（将候选组任务分配给具体用户）
     */
    @Operation(summary = "认领任务")
    @PostMapping("/task/claim")
    public String claimTask(@RequestParam String taskId, @RequestParam String userId) {
        processCommonService.claimTask(taskId, userId);
        return "任务已认领";
    }

    /**
     * 取消认领（释放任务回候选组）
     */
    @Operation(summary = "取消认领")
    @PostMapping("/task/unclaim")
    public String unclaimTask(@RequestParam String taskId) {
        processCommonService.unclaimTask(taskId);
        return "任务已取消认领";
    }

    // =====================================================================
    //  流程管理接口（由 ApprovalAdminService 提供）
    // =====================================================================

    /**
     * 临时改签（委派）- 将任务临时转给他人处理，完成后回到原处理人
     */
    @Operation(summary = "临时改签（委派）")
    @PostMapping("/admin/task/transfer-temporary")
    public String transferTemporary(@RequestParam String taskId,
                                     @RequestParam String targetUserId) {
        approvalAdminService.transferTemporary(taskId, targetUserId);
        return "任务已临时改签";
    }

    /**
     * 永久改签（转办）- 将任务永久转给他人处理
     */
    @Operation(summary = "永久改签（转办）")
    @PostMapping("/admin/task/transfer-permanent")
    public String transferPermanent(@RequestParam String taskId,
                                     @RequestParam String targetUserId) {
        approvalAdminService.transferPermanent(taskId, targetUserId);
        return "任务已永久改签";
    }

    /**
     * 加签 - 在当前审批节点增加审批人
     */
    @Operation(summary = "加签")
    @PostMapping("/admin/task/add-approver")
    public String addApprover(@RequestParam String taskId,
                               @RequestParam String newUserId) {
        approvalAdminService.addApprover(taskId, newUserId);
        return "已增加审批人";
    }

    /**
     * 去签 - 移除当前审批节点的审批人
     */
    @Operation(summary = "去签")
    @PostMapping("/admin/task/remove-approver")
    public String removeApprover(@RequestParam String taskId,
                                  @RequestParam String userId) {
        approvalAdminService.removeApprover(taskId, userId);
        return "已移除审批人";
    }

    /**
     * 管理-中止/暂停流程
     */
    @Operation(summary = "管理-中止暂停流程")
    @PostMapping("/admin/process/suspend")
    public String adminSuspendProcess(@RequestParam String processInstanceId,
                                       @RequestParam(required = false, defaultValue = "管理员暂停") String reason) {
        approvalAdminService.suspendProcess(processInstanceId, reason);
        return "流程已暂停（已通知提交人）";
    }

    /**
     * 管理-恢复已暂停的流程
     */
    @Operation(summary = "管理-恢复已暂停流程")
    @PostMapping("/admin/process/activate")
    public String adminActivateProcess(@RequestParam String processInstanceId) {
        approvalAdminService.activateProcess(processInstanceId);
        return "流程已恢复（已通知办理人）";
    }

    /**
     * 管理-终止流程
     */
    @Operation(summary = "管理-终止流程")
    @PostMapping("/admin/process/terminate")
    public String adminTerminateProcess(@RequestParam String processInstanceId,
                                         @RequestParam String reason) {
        approvalAdminService.terminateProcess(processInstanceId, reason);
        return "流程已终止（已通知提交人）";
    }

    /**
     * 管理-强制通过某个用户任务
     */
    @Operation(summary = "管理-强制通过任务")
    @PostMapping("/admin/task/force-complete")
    public String forceCompleteTask(@RequestParam String taskId,
                                     @RequestParam(required = false, defaultValue = "管理员强制通过") String comment) {
        approvalAdminService.forceCompleteTask(taskId, comment);
        return "任务已强制通过（已通知办理人）";
    }

    // =====================================================================
    //  流程列表 & 明细
    // =====================================================================

    /**
     * 查询所有流程列表（按流程定义Key枚举）
     */
    @Operation(summary = "查询所有流程列表")
    @GetMapping("/def/{processKey}/all")
    public List<Map<String, Object>> getAllProcessesByKey(@PathVariable String processKey) {
        ProcessKeyEnum key = ProcessKeyEnum.fromKey(processKey);
        return processService.queryAllProcesses(key);
    }

    /**
     * 查询流程审批明细
     */
    @Operation(summary = "查询流程审批明细")
    @GetMapping("/detail/{processInstanceId}")
    public Map<String, Object> getProcessDetail(@PathVariable String processInstanceId) {
        return processService.getProcessDetail(processInstanceId);
    }

    // =====================================================================
    //  流程图接口
    // =====================================================================

    /**
     * 通用流程图查看 - 根据流程定义Key枚举
     */
    @Operation(summary = "通用流程图（按流程定义Key）")
    @GetMapping(value = "/diagram/{processKey}", produces = {
            MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/svg+xml", MediaType.IMAGE_GIF_VALUE
    })
    public ResponseEntity<byte[]> getProcessDiagramByKey(
            @PathVariable String processKey,
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(required = false, defaultValue = "png") String format,
            @RequestParam(required = false, defaultValue = "completed") String highlightMode) {
        try {
            ProcessKeyEnum key = ProcessKeyEnum.fromKey(processKey);
            InputStream inputStream = processService.getProcessDiagram(
                    key, processInstanceId, format, highlightMode);
            byte[] imageBytes = inputStream.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(determineMediaType(format))
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/diagram", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, "image/svg+xml", MediaType.IMAGE_GIF_VALUE})
    public ResponseEntity<byte[]> getDiagram(
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(required = false, defaultValue = "png") String format,
            @RequestParam(required = false) String processKey) {
        try {
            // 默认使用请假流程
            ProcessKeyEnum key = (processKey != null && !processKey.isEmpty())
                    ? ProcessKeyEnum.fromKey(processKey)
                    : ProcessKeyEnum.LEAVE;
            var inputStream = processService.getProcessDiagram(key, processInstanceId, format);
            byte[] imageBytes = inputStream.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(determineMediaType(format))
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // =====================================================================
    //  兼容原接口（委托给新服务）
    // =====================================================================

    /**
     * 兼容 - 原单独部署请假流程
     */
    @PostMapping("/deploy/leave")
    @Operation(summary = "部署请假流程（兼容）")
    public String deployLeave() {
        processService.deployProcess(ProcessKeyEnum.LEAVE);
        return "请假流程已部署";
    }

    /**
     * 兼容 - 原单独部署多级审批流程
     */
    @PostMapping("/deploy/multi")
    @Operation(summary = "部署多级审批流程（兼容）")
    public String deployMulti() {
        processService.deployProcess(ProcessKeyEnum.MULTI_LEVEL_APPROVAL);
        return "多级审批流程已部署";
    }

    /**
     * 兼容 - 原单独查询所有请假流程
     */
    @Operation(summary = "查询所有请假流程（兼容）")
    @GetMapping("/all")
    public List<Map<String, Object>> getAllProcesses() {
        return processService.queryAllProcesses(ProcessKeyEnum.LEAVE);
    }

    /**
     * 兼容 - 原单独查询所有多级审批流程
     */
    @Operation(summary = "查询所有多级审批流程（兼容）")
    @GetMapping("/multi/all")
    public List<Map<String, Object>> getAllMultiProcesses() {
        return processService.queryAllProcesses(ProcessKeyEnum.MULTI_LEVEL_APPROVAL);
    }

    // =====================================================================
    //  工具方法
    // =====================================================================

    private MediaType determineMediaType(String format) {
        if (format == null) return MediaType.IMAGE_PNG;
        return switch (format.toLowerCase()) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "svg" -> MediaType.valueOf("image/svg+xml");
            case "gif" -> MediaType.IMAGE_GIF;
            default -> MediaType.IMAGE_PNG;
        };
    }
}