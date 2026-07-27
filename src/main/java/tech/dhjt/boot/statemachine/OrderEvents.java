package tech.dhjt.boot.statemachine;

/**
 * 订单状态机 - 事件定义
 */
public enum OrderEvents {
    /** 支付 */
    PAY,
    /** 发货 */
    DELIVER,
    /** 确认收货 */
    RECEIVE,
    /** 取消 */
    CANCEL
}
