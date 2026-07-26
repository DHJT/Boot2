package tech.dhjt.boot.convert.bean;

import lombok.Data;
import tech.dhjt.boot.convert.enums.TestLevelEnum;

/**
 * MapStruct 测试 - 目标对象
 * <p>
 * 包含 level 枚举字段（用于验证 String → 枚举转换）<br>
 * 包含 codeRef 字段（用于验证从 Map 中通过源 code 获取值）
 */
@Data
public class MapStructTarget {

    /** 从源 levelCode 转换而来的枚举值 */
    private TestLevelEnum level;

    /** 从 Map 中通过源 code 获取的值 */
    private String codeRef;

    /** 直接复制的名称 */
    private String name;

}