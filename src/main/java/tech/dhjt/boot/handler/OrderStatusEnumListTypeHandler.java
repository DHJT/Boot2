package tech.dhjt.boot.handler;

import tech.dhjt.boot.enums.OrderStatusEnum;

public class OrderStatusEnumListTypeHandler extends BaseEnumListTypeHandler<OrderStatusEnum> {

    public OrderStatusEnumListTypeHandler() {
        super(OrderStatusEnum.class);
    }

}
