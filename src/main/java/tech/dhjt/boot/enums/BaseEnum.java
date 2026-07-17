package tech.dhjt.boot.enums;

import lombok.Getter;

@Getter
public enum BaseEnum implements IEnum<Integer, String> {
    UNKNOWN(0, "未知");

    private final Integer code;

    private final String desc;

    BaseEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
