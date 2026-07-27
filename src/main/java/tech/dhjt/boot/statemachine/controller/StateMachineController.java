package tech.dhjt.boot.statemachine.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tech.dhjt.boot.handler.Result;
import tech.dhjt.boot.statemachine.OrderEvents;
import tech.dhjt.boot.statemachine.service.StateMachineDemoService;

/**
 * Spring StateMachine 功能演示接口
 */
@Tag(name = "状态机演示", description = "数据库持久化 / Choice / Junction / Deferred Event / 分层状态 / 并发状态")
@RestController
@RequestMapping("/api/statemachine")
@RequiredArgsConstructor
public class StateMachineController {

    private final StateMachineDemoService demoService;

    // ==================== 1. 数据库持久化 ====================

    @Operation(summary = "订单状态机发送事件（自动从数据库恢复并持久化）",
            description = "event 可选：PAY / DELIVER / RECEIVE / CANCEL；PAY 时可携带 amount 演示扩展状态持久化")
    @PostMapping("/order/{orderId}/events/{event}")
    public Result<Map<String, Object>> sendOrderEvent(
            @PathVariable String orderId,
            @PathVariable OrderEvents event,
            @RequestParam(required = false) Integer amount) throws Exception {
        return Result.success(demoService.sendOrderEvent(orderId, event, amount));
    }

    @Operation(summary = "查询订单状态机当前状态（从数据库恢复）")
    @GetMapping("/order/{orderId}")
    public Result<Map<String, Object>> getOrderState(@PathVariable String orderId) throws Exception {
        return Result.success(demoService.getOrderState(orderId));
    }

    // ==================== 2. Choice / Junction ====================

    @Operation(summary = "Choice 伪状态演示",
            description = "按金额动态路由：amount>=1000 -> HIGH，>=100 -> MEDIUM，否则 LOW")
    @PostMapping("/choice")
    public Result<Map<String, Object>> choice(@RequestParam int amount) {
        return Result.success(demoService.choiceDemo(amount));
    }

    @Operation(summary = "Junction 伪状态演示",
            description = "按评分路由：score>=90 -> APPROVED，>=60 -> MANUAL_REVIEW，否则 REJECTED")
    @PostMapping("/junction")
    public Result<Map<String, Object>> junction(@RequestParam int score) {
        return Result.success(demoService.junctionDemo(score));
    }

    // ==================== 3. Deferred Event ====================

    @Operation(summary = "Deferred Event 演示",
            description = "BUSY 状态下 TASK 事件被延迟，回到 IDLE 后自动重放")
    @PostMapping("/defer/demo")
    public Result<Map<String, Object>> deferDemo() {
        return Result.success(demoService.deferDemo());
    }

    // ==================== 4. 分层状态 ====================

    @Operation(summary = "分层状态演示",
            description = "PROCESSING 父状态包含 VALIDATING/PACKING 子状态，父状态上的转移在子状态下也可触发")
    @PostMapping("/hierarchical/demo")
    public Result<Map<String, Object>> hierarchicalDemo() {
        return Result.success(demoService.hierarchicalDemo());
    }

    // ==================== 5. 并发状态（Fork/Join） ====================

    @Operation(summary = "并发状态（Fork/Join）演示",
            description = "fork 进入两个并行 Region，两个 Region 都完成后 join 汇合到 DONE")
    @PostMapping("/forkjoin/demo")
    public Result<Map<String, Object>> forkJoinDemo() {
        return Result.success(demoService.forkJoinDemo());
    }
}
