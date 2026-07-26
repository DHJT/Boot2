package tech.dhjt.boot.convert;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import tech.dhjt.boot.convert.bean.MapStructSource;
import tech.dhjt.boot.convert.bean.MapStructTarget;
import tech.dhjt.boot.convert.enums.TestLevelEnum;
import tech.dhjt.boot.enums.IEnum;

import java.util.Map;
import java.util.Objects;

/**
 * MapStruct 测试转换器
 * <p>
 * 演示两种功能：
 * 1. 使用枚举接口(IEnum)进行 String → 枚举值 的转换
 * 2. 传入 Map 参数，使用源对象的 code 进行获取值并赋值到目标对象的 codeRef 上
 */
@Mapper(imports = {TestLevelEnum.class})
public interface MapStructTestConverter {

    MapStructTestConverter INSTANCE = Mappers.getMapper(MapStructTestConverter.class);

    /**
     * 将源对象转换为目标对象，同时接收一个 Map 参数用于 codeRef 的查找
     * <p>
     * - level 通过 expression 调用 stringToTestLevelEnum 方法将 levelCode 转为枚举
     * - codeRef 通过 expression 从 codeMap 中获取 source.code 对应的值
     * - name 直接映射自 source.name
     */
//    @Mapping(target = "level", expression = "java(stringToTestLevelEnum1(source.getLevelCode()))")
//    @Mapping(target = "level", expression = "java(stringToTestLevelEnum(TestLevelEnum.class, source.getLevelCode()))")
    @Mapping(target = "level", qualifiedByName = "stringToTestLevelEnum(TestLevelEnum.class, source.getLevelCode())")
    @Mapping(target = "codeRef", expression = "java(codeMap.get(source.getCode()))")
    MapStructTarget toTarget(MapStructSource source, Map<String, String> codeMap);

    /**
     * 将 String 类型转换为实现了 IEnum 接口的枚举
     * <p>
     * 根据 code 值查找对应的 TestLevelEnum 枚举常量。
     * TestLevelEnum 实现了 IEnum<String, String> 接口，通过 getCode() 进行匹配。
     */
    @Named("stringToTestLevelEnum")
    default <K, V, E extends Enum<E> & IEnum<K, V>> E stringToTestLevelEnum(Class<E> enumClass, K levelCode) {
        return IEnum.fromValue(enumClass, levelCode);
    }

    @Named("stringToTestLevelEnum1")
    default TestLevelEnum stringToTestLevelEnum1(String levelCode) {
        return TestLevelEnum.fromCode(levelCode);
    }

    // ========== 以下为备用方法：演示通过 IEnum 接口的 getCode() 自动匹配 ==========

    /**
     * 通用方法：根据 String code 查找实现了 IEnum<String, ?> 接口的枚举。
     * 如果 MapStruct 无法自动匹配，可借助本方法。
     * <p>
     * 注意：此方法为 default，MapStruct 会将其视为候选转换方法。
     * 实际匹配优先级：类型匹配 > @Named > 同类型方法。
     */
    default <T extends Enum<T> & IEnum<String, ?>> T fromCode(String code, Class<T> enumClass) {
        if (code == null) {
            return null;
        }
        for (T constant : enumClass.getEnumConstants()) {
            if (Objects.equals(constant.getCode(), code)) {
                return constant;
            }
        }
        return null;
    }

}