package tech.dhjt.boot.enums;

import lombok.Getter;

@Getter
public enum BaseEnum implements IEnum<Integer, String> {
    UNKNOWN(0, "未知", "未知") {
        @Override
        public boolean defaultFlag() {
            return true;
        }
    };

    private final Integer code;

    private final String desc;

    private final String name;

    BaseEnum(Integer code, String name, String desc) {
        this.code = code;
        this.name = name;
        this.desc = desc;
    }

}
