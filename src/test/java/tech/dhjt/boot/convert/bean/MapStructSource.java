package tech.dhjt.boot.convert.bean;

import lombok.Data;

/**
 * MapStruct 测试 - 源对象
 * <p>
 * 包含 code 字段，用于通过 Map 参数查找对应的 codeRef 值
 */
@Data
public class MapStructSource {

    /** 用于从 Map 中查找 codeRef 的键 */
    private String code;

    /** 枚举级别的代码值 (String) */
    private String levelCode;

    /** 名称 */
    private String name;

}