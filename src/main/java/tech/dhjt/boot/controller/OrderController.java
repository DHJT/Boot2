package tech.dhjt.boot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.dhjt.boot.bean.Order;
import tech.dhjt.boot.bean.OrderRelateThirdInfo;
import tech.dhjt.boot.bean.dto.OrderDTO;
import tech.dhjt.boot.config.TenantContext;
import tech.dhjt.boot.convert.OrderConvert;
import tech.dhjt.boot.enums.OrderStatusEnum;
import tech.dhjt.boot.service.OrderService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单 Controller - 接收对象（含枚举和LocalDateTime）并保存到数据库
 */
@Tag(name = "订单测试", description = "订单首页及测试接口")
@Slf4j
@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderConvert orderConvert;

    @Operation(summary = "首页listByStatus", description = "listByStatus")
    @GetMapping("/listByStatus")
    public ResponseEntity<List<Order>> listByStatus(@NotNull OrderStatusEnum orderstatus) {
        List<Order> list = orderService.lambdaQuery().eq(Order::getStatus, orderstatus).list();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "updateById", description = "updateById")
    @PutMapping("/updateById")
    public ResponseEntity<Object> updateById(@Valid @RequestBody OrderDTO updateOrder) {
        Long id = updateOrder.getId();
        Order oldOrder = orderService.lambdaQuery().eq(Order::getId, id).one();

        Assert.notNull(oldOrder, "数据不存在，确认后再更新");

        orderConvert.toBeanForUpdate(updateOrder, oldOrder);
        boolean result = orderService.updateById(oldOrder);
        if (result) {
            return ResponseEntity.ok(oldOrder);
        }
        return ResponseEntity.status(901).body("更新失败");
    }

//    public boolean updateWithRetry(Order entity, int maxRetries) {
//        for (int i = 0; i < maxRetries; i++) {
//            // 1. 重新查询最新数据（包含最新版本号）
//            YourEntity latest = yourMapper.selectById(entity.getId());
//            // 2. 将需要更新的字段复制到最新实体上
//            latest.setSomeField(entity.getSomeField());
//            // 3. 尝试更新
//            if (yourMapper.updateById(latest) > 0) {
//                return true; // 更新成功
//            }
//            // 4. 更新失败，等待一小段时间后重试
//            try { Thread.sleep(100); } catch (InterruptedException e) { /* ... */ }
//        }
//        return false; // 重试次数用尽，更新失败
//    }

    /**
     * 接收订单对象并保存
     *
     * 请求示例 JSON:
     * {
     *   "orderNo": "ORD20260711001",
     *   "customerName": "张三",
     *   "amount": 99.99,
     *   "status": "PENDING",
     *   "orderTime": "2026-07-11T11:30:00"
     * }
     */
    @Operation(summary = "createOrder", description = "createOrder")
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderDTO orderDTO) {
        TenantContext.setCurrentTenant(0L);

        Order order = orderConvert.toBeanForAdd(orderDTO);

        Map<String, Object> extraInfo = new HashMap<>();
        extraInfo.put("key", "123");
        extraInfo.put("time", LocalDateTime.now());
        order.setExtraInfo(extraInfo);

        OrderRelateThirdInfo thirdInfo = OrderRelateThirdInfo.builder().info("测试：" + LocalDateTime.now().toString()).thirdNo("1").build();

        order.setThirdInfos(List.of(thirdInfo, thirdInfo));
        Order saved = orderService.save1(order);
        TenantContext.clear();
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "delete", description = "delete")
    /** 逻辑删除 */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        TenantContext.setCurrentTenant(0L);
        boolean remove = orderService.lambdaUpdate().eq(Order::getId, id).remove();
        TenantContext.clear();
        if (remove) {
            return "deleted";
        }
        return "error";
    }

    @Operation(summary = "selectPageByStatus", description = "selectPageByStatus")
    @GetMapping("/selectPageByStatus/{pageNo}/{pageSize}/{status}")
    public ResponseEntity<IPage<Order>> selectPageByStatus(@PathVariable Integer pageNo, @PathVariable Integer pageSize, @PathVariable OrderStatusEnum status) {
        IPage<Order> orderIPage = orderService.selectPageByStatus(pageNo, pageSize, status);

        return ResponseEntity.ok(orderIPage);
    }

}