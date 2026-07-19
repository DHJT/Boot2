package tech.dhjt.boot.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Objects;

/**
 * 邮箱类型枚举
 * <p>
 * 1-固定邮箱 2-业务和客户邮箱 3-订单邮箱
 */
@Getter
public enum EmailTypeEnum {

    FIXED_EMAIL(1, "固定邮箱"),
    BUSINESS_CUSTOMER_EMAIL(2, "业务和客户邮箱"),
    ORDER_EMAIL(3, "订单邮箱"),
    UNKNOWN(0, "未知");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String desc;

    EmailTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonCreator
    public static EmailTypeEnum fromValue(Integer value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (EmailTypeEnum type : EmailTypeEnum.values()) {
            if (Objects.equals(type.code, value)) {
                return type;
            }
        }
        return UNKNOWN;
    }

}