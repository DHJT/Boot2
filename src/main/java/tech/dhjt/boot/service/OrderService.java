package tech.dhjt.boot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.dhjt.boot.bean.Order;
import tech.dhjt.boot.enums.OrderStatusEnum;
import tech.dhjt.boot.mapper.OrderMapper;

/**
 * 订单 Service
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    private final OrderMapper orderMapper;

    /**
     * 保存订单
     */
    public Order save1(Order order) {
        orderMapper.insert(order);
        return order;
    }

    /**
     * 根据状态分页查询（子查询方式：先查分页ID，再查完整数据）
     *
     * @param current 当前页（从1开始）
     * @param size    每页条数
     * @param status  订单状态
     * @return 分页数据
     */
    public IPage<Order> selectPageByStatus(long current, long size, OrderStatusEnum status) {
        // 1. 查询总数
        Long total = orderMapper.selectCountByStatus(status);
        total = total == null ? 0L : total;

        // 2. 创建分页对象
        Page<Order> page = new Page<>(current, size, total);
        if (total == 0) {
            return page;
        }

        // 3. 子查询分页：手动计算 offset
        long offset = (current - 1) * size;
//        page.setRecords(orderMapper.selectPageByStatusWithSubQuery1(status, offset, size));

        // 4. 设置排序信息（与 SQL 中的 ORDER BY id 保持一致）
        page.addOrder(OrderItem.asc("id"));

        return orderMapper.selectPageByStatusWithSubQuery(page, status);
    }

}