package tech.dhjt.boot3.controller;

import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.dhjt.boot3.model.dto.ProcessDefinitionDTO;
import tech.dhjt.boot3.service.flowable.LeaveService;
import tech.dhjt.boot3.service.flowable.MultiLevelApprovalProcessService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Flowable 工作流示例 — 请假审批流程 + 多级审批流程 REST API
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/flowable")
public class FlowableController {

    private final LeaveService leaveService;

    private final MultiLevelApprovalProcessService multiLevelApprovalProcessService;

    private final RepositoryService repositoryService;

    // ===== 通用接口 =====

    /**
     * 查询已部署的流程定义
     */
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

    // ===== 请假流程 (leaveProcess) =====

    /**
     * 手动部署请假流程
     */
    @PostMapping("/deploy")
    public String deploy() {
        leaveService.deployProcess();
        return "请假流程已部署";
    }

    /**
     * 启动请假流程
     */
    @PostMapping("/start")
    public String startLeave(@RequestParam String applicantName,
                             @RequestParam String reason,
                             @RequestParam Integer days) {
        return leaveService.startLeaveProcess(applicantName, reason, days);
    }

    /**
     * 按用户组查询请假待办任务
     */
    @GetMapping("/tasks/{group}")
    public List<Map<String, Object>> getTasksByGroup(@PathVariable String group) {
        return leaveService.queryTasksByGroup(group);
    }

    /**
     * 审批请假任务（使用 boolean approved）
     */
    @PostMapping("/complete")
    public String completeTask(@RequestParam String taskId,
                               @RequestParam Boolean approved,
                               @RequestParam(required = false) String comment) {
        leaveService.completeTask(taskId, approved, comment);
        return "任务已完成";
    }

    /**
     * 查询所有请假流程列表（运行中+已结束），每个流程附带审批时间线
     */
    @GetMapping("/all")
    public List<Map<String, Object>> getAllProcesses() {
        return leaveService.queryAllProcesses();
    }

    /**
     * 获取请假流程审批明细
     */
    @GetMapping("/detail/{processInstanceId}")
    public Map<String, Object> getProcessDetail(@PathVariable String processInstanceId) {
        return leaveService.getProcessDetail(processInstanceId);
    }

    /**
     * 获取流程图（默认请假流程）
     */
    @GetMapping(value = "/diagram", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getDiagram(@RequestParam(required = false) String processInstanceId) {
        try {
            var inputStream = leaveService.getProcessDiagram(processInstanceId);
            byte[] imageBytes = inputStream.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ===== 多级复杂审批流程 (multiLevelApprovalProcess) =====

    /**
     * 手动部署多级审批流程
     */
    @PostMapping("/multi/deploy")
    public String deployMulti() {
        multiLevelApprovalProcessService.deployProcess();
        return "多级审批流程已部署";
    }

    /**
     * 启动多级审批流程
     */
    @PostMapping("/multi/start")
    public String startMulti(@RequestParam String applicantName,
                             @RequestParam String reason,
                             @RequestParam Integer days) {
        return multiLevelApprovalProcessService.startProcess(applicantName, reason, days);
    }

    /**
     * 按用户组查询多级审批待办任务
     */
    @GetMapping("/multi/tasks/{group}")
    public List<Map<String, Object>> getMultiTasksByGroup(@PathVariable String group) {
        return multiLevelApprovalProcessService.queryTasksByGroup(group);
    }

    /**
     * 审批多级审批任务（使用 String approval: 'approved'/'rejected'）
     */
    @PostMapping("/multi/complete")
    public String completeMultiTask(@RequestParam String taskId,
                                    @RequestParam String approval,
                                    @RequestParam(required = false) String comment) {
        multiLevelApprovalProcessService.completeTask(taskId, approval, comment);
        return "任务已完成";
    }

    /**
     * 查询所有多级审批流程列表
     */
    @GetMapping("/multi/all")
    public List<Map<String, Object>> getAllMultiProcesses() {
        return multiLevelApprovalProcessService.queryAllProcesses();
    }

    /**
     * 获取多级审批流程明细
     */
    @GetMapping("/multi/detail/{processInstanceId}")
    public Map<String, Object> getMultiProcessDetail(@PathVariable String processInstanceId) {
        return multiLevelApprovalProcessService.getProcessDetail(processInstanceId);
    }

    /**
     * 获取多级审批流程图
     */
    @GetMapping(value = "/multi/diagram", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getMultiDiagram(@RequestParam(required = false) String processInstanceId) {
        try {
            var inputStream = multiLevelApprovalProcessService.getProcessDiagram(processInstanceId);
            byte[] imageBytes = inputStream.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}