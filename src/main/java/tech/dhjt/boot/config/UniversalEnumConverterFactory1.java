package tech.dhjt.boot.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 兼容 String、Integer、Long 等不同类型枚举值的自动转换，核心思路是：</br></br>
 * <p>
 * 1. 所有请求参数到达后端时都是字符串（如 ?status=1 或 ?type=A），因此我们只需注册一个 String -> Enum 的通用 ConverterFactory。</br>
 * 2. 在该工厂中，针对每个枚举类找到其 @JsonCreator 静态工厂方法，获取该方法的第一个参数类型（可能是 String、Integer、Long 等）。</br>
 * 3. 转换时，先根据参数类型把字符串 source 转换成相应的数字/布尔/字符串，再反射调用工厂方法，得到枚举常量。
 * </p>
 */
public class UniversalEnumConverterFactory1 implements ConverterFactory<String, Enum<?>> {

    private static final Map<Class<?>, Method> CREATOR_METHOD_CACHE = new ConcurrentHashMap<>();

    @Override
    public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
        Method creator = findCreatorMethod(targetType);
        return new UniversalEnumConverter<>(targetType, creator);
    }

    // 查找带 @JsonCreator 的静态方法，记录其参数类型
    private static Method findCreatorMethod(Class<?> enumClass) {
        return CREATOR_METHOD_CACHE.computeIfAbsent(enumClass, clazz ->
                Arrays.stream(clazz.getDeclaredMethods())
                        .filter(m -> Modifier.isStatic(m.getModifiers()))
                        .filter(m -> m.getAnnotation(JsonCreator.class) != null)
                        .filter(m -> m.getParameterCount() == 1)          // 只支持单参数工厂
                        .findFirst()
                        .orElseGet(() -> {                                 // 后备：常规命名方法
                            try {
                                return clazz.getDeclaredMethod("fromValue", String.class);
                            } catch (NoSuchMethodException e) {
                                try {
                                    return clazz.getDeclaredMethod("from", String.class);
                                } catch (NoSuchMethodException ex) {
                                    return null;
                                }
                            }
                        })
        );
    }

    private static class UniversalEnumConverter<T extends Enum<?>> implements Converter<String, T> {
        private final Class<T> enumType;
        private final Method creatorMethod;

        public UniversalEnumConverter(Class<T> enumType, Method creatorMethod) {
            this.enumType = enumType;
            this.creatorMethod = creatorMethod;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T convert(String source) {
            if (source == null || source.isEmpty()) {
                return null;
            }
            // 没有自定义工厂方法，回退到 Spring 默认的 Enum.valueOf
            if (creatorMethod == null) {
                return (T) Enum.valueOf((Class) enumType, source);
            }

            Class<?> paramType = creatorMethod.getParameterTypes()[0];
            try {
                Object arg = convertSourceToTargetType(source, paramType);
                return (T) creatorMethod.invoke(null, arg);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Cannot convert '" + source + "' to " + enumType.getSimpleName(), e);
            }
        }

        // 根据工厂方法参数类型，把字符串转换成对应的类型
        private Object convertSourceToTargetType(String source, Class<?> targetType) {
            if (targetType == String.class) {
                return source;
            }
            if (targetType == Integer.class || targetType == int.class) {
                return Integer.valueOf(source);
            }
            if (targetType == Long.class || targetType == long.class) {
                return Long.valueOf(source);
            }
            if (targetType == Short.class || targetType == short.class) {
                return Short.valueOf(source);
            }
            if (targetType == Byte.class || targetType == byte.class) {
                return Byte.valueOf(source);
            }
            if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.valueOf(source);
            }
            if (targetType == Double.class || targetType == double.class) {
                return Double.valueOf(source);
            }
            if (targetType == Float.class || targetType == float.class) {
                return Float.valueOf(source);
            }
            if (targetType == BigInteger.class) {
                return new BigInteger(source);
            }
            if (targetType == BigDecimal.class) {
                return new BigDecimal(source);
            }
            // 不支持的类型，直接抛出异常
            throw new IllegalArgumentException("Unsupported enum creator parameter type: " + targetType);
        }
    }
}
