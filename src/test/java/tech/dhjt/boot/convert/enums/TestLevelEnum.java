package tech.dhjt.boot.convert.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import tech.dhjt.boot.enums.IEnum;
import tech.dhjt.boot.enums.IGroup;
import tech.dhjt.boot.enums.IOrder;

import java.util.Objects;

/**
 * 测试用枚举 - 实现 IEnum<String, String> 接口
 * <p>
 * 用于验证 MapStruct 通过 IEnum 接口进行 String → 枚举值 的转换
 */
@Getter
public enum TestLevelEnum implements IEnum<String, String>, IGroup, IOrder {

    LOW("L", "低级", "低级", "低级", 1),
    MEDIUM("M", "中级", "中级", "中级", 2),
    HIGH("H", "高级", "高级", "高级", 3),
    UNKNOWN("UNKNOWN", "未知", "未知", "未知", 0);

    @JsonValue
    private final String code;

    private final String name;

    private final String desc;

    private final String subGroup;

    private final int order;

    TestLevelEnum(String code, String name, String desc, String subGroup, int order) {
        this.code = code;
        this.name = name;
        this.desc = desc;
        this.subGroup = subGroup;
        this.order = order;
    }

    /**
     * 根据 code 查找对应的枚举值
     */
    @JsonCreator
    public static TestLevelEnum fromCode(String code) {
        var testLevelEnum = IEnum.fromValue(TestLevelEnum.class, code);
        return testLevelEnum == null ? UNKNOWN : testLevelEnum;
    }

    @Override
    public String getGroup() {
        return "TestLevel";
    }

    @Override
    public String getSubGroup() {
        return subGroup;
    }
}