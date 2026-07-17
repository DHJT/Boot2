package tech.dhjt.boot.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum GenderEnum implements IEnum<Integer, String> {
    MALE(1, "男"),
    FEMALE(2, "女"),
    UNKNOWN(0, "未知");

    @EnumValue      // 标记数据库存储的值
    @JsonValue      // JSON序列化时输出的值
    private final Integer code;
    private final String desc;

    GenderEnum(Integer value, String desc) {
        this.code = value;
        this.desc = desc;
    }

    // 反序列化时根据value获取枚举（接收JSON请求时使用）
    @JsonCreator
    public static GenderEnum fromValue(int value) {
        for (GenderEnum gender : GenderEnum.values()) {
            if (gender.code == value) {
                return gender;
            }
        }
        return UNKNOWN; // 或者抛异常
    }
}
