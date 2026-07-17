package tech.dhjt.boot.config;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用枚举转换工厂：支持 String -> Enum 自动转换。
 * 转换策略按优先级：
 * 1. 枚举中标注 @JsonCreator 的静态方法
 * 2. 枚举中名为 fromValue(String) 或 from(String) 的静态方法
 * 3. 枚举中标注 @EnumValue 的字段（循环匹配）
 * 4. 默认 Enum.valueOf(name)
 */
public class UniversalEnumConverterFactory12 implements ConverterFactory<String, Enum<?>> {

    private static final Map<Class<?>, Method> CREATOR_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> ENUM_VALUE_FIELD_CACHE = new ConcurrentHashMap<>();

    @Override
    public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
        Method creatorMethod = findCreatorMethod(targetType);
        Field enumValueField = findEnumValueField(targetType);
        return new UniversalEnumConverter<>(targetType, creatorMethod, enumValueField);
    }

    /**
     * 查找用于字符串 -> 枚举的静态工厂方法。
     */
    private static Method findCreatorMethod(Class<?> enumClass) {
        return CREATOR_METHOD_CACHE.computeIfAbsent(enumClass, clazz -> {
            // 1. 寻找 @JsonCreator 注解的静态方法
            Method method = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> Modifier.isStatic(m.getModifiers()))
                    .filter(m -> m.getAnnotation(JsonCreator.class) != null)
                    .findFirst()
                    .orElse(null);
            if (method != null) {
                return method;
            }
            // 2. 寻找约定名称的静态方法 fromValue(String) 或 from(String)
            try {
                return clazz.getDeclaredMethod("fromValue", String.class);
            } catch (NoSuchMethodException e) {
                try {
                    return clazz.getDeclaredMethod("from", String.class);
                } catch (NoSuchMethodException ex) {
                    return null; // 未找到
                }
            }
        });
    }

    /**
     * 查找枚举中被 @EnumValue 标注的字段。
     */
    private static Field findEnumValueField(Class<?> enumClass) {
        return ENUM_VALUE_FIELD_CACHE.computeIfAbsent(enumClass, clazz -> {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getAnnotation(EnumValue.class) != null) {
                    field.setAccessible(true);
                    return field;
                }
            }
            return null;
        });
    }

    /**
     * 实际的转换器实现。
     */
    private static class UniversalEnumConverter<T extends Enum<?>> implements Converter<String, T> {
        private final Class<T> enumType;
        private final Method creatorMethod;
        private final Field enumValueField;

        UniversalEnumConverter(Class<T> enumType, Method creatorMethod, Field enumValueField) {
            this.enumType = enumType;
            this.creatorMethod = creatorMethod;
            this.enumValueField = enumValueField;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T convert(String source) {
            if (source == null || source.isEmpty()) {
                return null;
            }
            // 第一优先级：通过静态工厂方法转换
            if (creatorMethod != null) {
                try {
                    return (T) creatorMethod.invoke(null, source);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Cannot convert " + source + " to " + enumType.getSimpleName(), e);
                }
            }
            // 第二优先级：通过 @EnumValue 字段匹配
            if (enumValueField != null) {
                for (T constant : enumType.getEnumConstants()) {
                    try {
                        Object fieldValue = enumValueField.get(constant);
                        if (source.equals(String.valueOf(fieldValue))) {
                            return constant;
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to access @EnumValue field", e);
                    }
                }
                throw new IllegalArgumentException(
                        "No enum constant with @EnumValue " + source + " in " + enumType.getSimpleName());
            }
            // 第三优先级：回退到 Spring 默认的 Enum.valueOf，按枚举名称匹配
            return (T) Enum.valueOf((Class) enumType, source);
        }
    }
}
