package tech.dhjt.boot.statemachine;

/**
 * 订单状态机 - 状态定义
 */
public enum OrderStates {
    /** 待支付 */
    PENDING,
    /** 已支付 */
    PAID,
    /** 已发货 */
    DELIVERED,
    /** 已完成 */
    COMPLETED,
    /** 已取消 */
    CANCELLED
}
