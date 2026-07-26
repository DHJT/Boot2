package tech.dhjt.boot.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum GenderEnum implements IEnum<Integer, String>, IGroup, IOrder {
    MALE(1, "男", "男", 1, "性别"),
    FEMALE(2, "女", "女", 2, "性别"),
    UNKNOWN(0, "未知", "未知", 0, "性别") {
        @Override
        public boolean defaultFlag() {
            return true;
        }
    };

    @EnumValue      // 标记数据库存储的值
    @JsonValue      // JSON序列化时输出的值
    private final Integer code;
    private final String name;
    private final String desc;
    private final int order;
    private final String subGroup;

    GenderEnum(Integer value, String name, String desc, int order, String subGroup) {
        this.code = value;
        this.name = name;
        this.desc = desc;
        this.order = order;
        this.subGroup = subGroup;
    }

    // 反序列化时根据value获取枚举（接收JSON请求时使用）
    @JsonCreator
    public static GenderEnum fromValue(int value) {
        var genderEnum = IEnum.fromValue(GenderEnum.class, value);
        return genderEnum == null ? UNKNOWN : genderEnum; // 或者抛异常
    }

    @JsonCreator
    public static GenderEnum fromValue(Integer value) {
        return IEnum.fromValue(GenderEnum.class, value);
    }

    @Override
    public String getGroup() {
        return "GenderEnum";
    }
}
