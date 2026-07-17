package tech.dhjt.boot.convert;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import tech.dhjt.boot.enums.OrderStatusEnum;

import java.util.Objects;

//@Component  // 让 Spring 自动发现并注册
public class StringToOrderStatusEnumConverter implements Converter<String, OrderStatusEnum> {

    @Override
    public OrderStatusEnum convert(String source) {
        for (OrderStatusEnum order : OrderStatusEnum.values()) {
            if (Objects.equals(order.getCode(), source)) {
                return order;
            }
        }
        // 可抛异常或返回默认值
        throw new IllegalArgumentException("无效的状态码: " + source);
    }
}