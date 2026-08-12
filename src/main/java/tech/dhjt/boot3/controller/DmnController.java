package tech.dhjt.boot3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.dhjt.boot3.service.dmn.DmnService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * DMN 决策管理 REST API — 提供 DMN 决策表的部署、评估、查询等管理接口
 *
 * @author DHJT
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/dmn")
@Tag(name = "DMN 决策管理", description = "DMN 决策表的部署、评估、查询等管理接口")
public class DmnController {

    private final DmnService dmnService;

    // =====================================================================
    //  决策表部署
    // =====================================================================

    /**
     * 部署所有预定义的 DMN 决策表
     */
    @Operation(summary = "部署所有DMN决策表", description = "部署 classpath 下预定义的请假天数决策表和部门决策表")
    @PostMapping("/deploy/all")
    public String deployAll() {
        dmnService.deployAll();
        return "所有DMN决策表已部署";
    }

    /**
     * 部署单个 DMN 决策表
     */
    @Operation(summary = "部署单个DMN决策表", description = "根据资源路径部署单个DMN决策表")
    @PostMapping("/deploy")
    public String deploy(
            @Parameter(description = "DMN资源路径，如 processes/leaveDaysDecision.dmn") @RequestParam String resourcePath) {
        String deploymentId = dmnService.deployDecision(resourcePath);
        return "DMN决策表已部署，部署ID: " + deploymentId;
    }

    /**
     * 上传并部署 DMN 文件（支持 .dmn 文件上传）
     */
    @Operation(summary = "上传并部署DMN文件", description = "接收 DMN 格式的文件（.dmn），保存并部署到 Flowable DMN 引擎")
    @PostMapping(value = "/deploy/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadAndDeploy(
            @Parameter(description = "DMN 文件（.dmn 格式）") @RequestParam("file") MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                throw new IllegalArgumentException("文件名不能为空");
            }
            String deploymentId = dmnService.deployDmnFile(originalFilename, file.getInputStream());
            return Map.of(
                    "success", true,
                    "message", "DMN 文件已部署成功",
                    "deploymentId", deploymentId,
                    "fileName", originalFilename
            );
        } catch (IOException e) {
            throw new RuntimeException("读取上传的 DMN 文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通过 XML 内容部署 DMN 决策表
     */
    @Operation(summary = "通过 XML 内容部署DMN", description = "接收 DMN 格式的 XML 字符串内容，保存并部署到 Flowable DMN 引擎")
    @PostMapping("/deploy/content")
    public Map<String, Object> deployByContent(
            @Parameter(description = "资源名称，如 myDecision.dmn") @RequestParam String resourceName,
            @Parameter(description = "DMN 的 XML 字符串内容") @RequestBody String dmnContent) {
        String deploymentId = dmnService.deployDmnContent(resourceName, dmnContent);
        return Map.of(
                "success", true,
                "message", "DMN 内容已部署成功",
                "deploymentId", deploymentId,
                "resourceName", resourceName
        );
    }

    // =====================================================================
    //  决策评估
    // =====================================================================

    /**
     * 评估请假天数决策
     */
    @Operation(summary = "评估请假天数决策", description = "根据请假天数评估请假类别（short/long），判断是否需要院长审批")
    @GetMapping("/evaluate/days")
    public Map<String, Object> evaluateDays(
            @Parameter(description = "请假天数") @RequestParam int days) {
        return dmnService.evaluateLeaveDays(days);
    }

    /**
     * 评估部门决策
     */
    @Operation(summary = "评估部门决策", description = "根据提交人的部门判断是否需要院长审批")
    @GetMapping("/evaluate/department")
    public Map<String, Object> evaluateDepartment(
            @Parameter(description = "部门名称") @RequestParam String deptName) {
        return dmnService.evaluateDepartment(deptName);
    }

    /**
     * 综合评估（天数 + 部门）
     */
    @Operation(summary = "综合评估（天数+部门）", description = "结合请假天数和部门进行综合决策，给出最终审批建议")
    @GetMapping("/evaluate/combined")
    public Map<String, Object> evaluateCombined(
            @Parameter(description = "请假天数") @RequestParam int days,
            @Parameter(description = "部门名称") @RequestParam String deptName) {
        return dmnService.evaluateCombined(days, deptName);
    }

    /**
     * 评估审批路径决策（天数 + 部门 → 辅导员/院长）
     */
    @Operation(summary = "评估审批路径决策", description = "根据请假天数与部门判定审批路径（advisor/dean），请假流程路由的单一决策来源")
    @GetMapping("/evaluate/approval-path")
    public Map<String, Object> evaluateApprovalPath(
            @Parameter(description = "请假天数") @RequestParam int days,
            @Parameter(description = "部门名称") @RequestParam String deptName) {
        return dmnService.evaluateApprovalPath(days, deptName);
    }

    // =====================================================================
    //  决策表查询
    // =====================================================================

    /**
     * 查询所有已部署的决策表
     */
    @Operation(summary = "查询所有已部署决策表", description = "获取所有已部署的DMN决策表列表（最新版本）")
    @GetMapping("/decisions")
    public List<Map<String, Object>> listDecisions() {
        return dmnService.listDecisions();
    }

    /**
     * 根据决策Key查询决策表详情
     */
    @Operation(summary = "查询决策表详情", description = "根据决策Key查询决策表的详细信息，包括输入输出字段和规则")
    @GetMapping("/decisions/{decisionKey}")
    public Map<String, Object> getDecisionDetail(
            @Parameter(description = "决策Key，如 leaveDaysDecision / leaveDepartmentDecision") @PathVariable String decisionKey) {
        return dmnService.getDecisionDetail(decisionKey);
    }
}