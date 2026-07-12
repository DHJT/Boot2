package tech.dhjt.boot.enums;

public enum BaseEnum implements IEnum<Integer> {
    UNKNOWN(0, "未知");

    private final Integer code;

    private final String desc;

    BaseEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getCode() {
        return code;
    }
}
