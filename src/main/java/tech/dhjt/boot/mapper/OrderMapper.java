package tech.dhjt.boot.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import tech.dhjt.boot.bean.Order;
import tech.dhjt.boot.enums.OrderStatusEnum;

import java.util.List;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 根据状态分页查询（子查询方式：先查分页ID，再查完整数据）
     *
     * @param status 订单状态
     * @param offset 偏移量
     * @param size   每页条数
     * @return 订单列表
     */
    List<Order> selectPageByStatusWithSubQuery1(@Param("status") OrderStatusEnum status,
                                               @Param("offset") long offset,
                                               @Param("size") long size);

    @InterceptorIgnore
    IPage<Order> selectPageByStatusWithSubQuery(@Param("page") IPage<Order> page,
                                               @Param("status") OrderStatusEnum status);

    /**
     * 根据状态统计总数
     *
     * @param status 订单状态
     * @return 总数
     */
    Long selectCountByStatus(@Param("status") OrderStatusEnum status);
}