package tech.dhjt.boot.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Objects;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatusEnum {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消"),
    UNKNOWN("UNKNOWN", "未知");

    @EnumValue // MyBatis-Plus 存储到数据库的值
    @JsonValue // Jackson 序列化时的值
    private final String code;

    private final String desc;

    OrderStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 反序列化时根据value获取枚举（接收JSON请求时使用）
    @JsonCreator
    public static OrderStatusEnum fromValue(String value) {
        for (OrderStatusEnum order : OrderStatusEnum.values()) {
            if (Objects.equals(order.code, value)) {
                return order;
            }
        }
        return UNKNOWN; // 或者抛异常
    }

}