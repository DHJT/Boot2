package tech.dhjt.boot.config;


import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 所有通过 Spring MVC 参数绑定（@RequestParam、@PathVariable、@ModelAttribute 等）的枚举字段，都会自动使用你的 fromValue 或 @JsonCreator 方法进行转换，
 * 无需再为每个枚举单独写 Converter。
 */
public class UniversalEnumConverterFactory implements ConverterFactory<String, Enum<?>> {

    // 缓存每个枚举类型的转换方法，避免重复反射
    private static final Map<Class<?>, Method> CREATOR_METHOD_CACHE = new ConcurrentHashMap<>();

    @Override
    public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
        return new UniversalEnumConverter<>(targetType, findCreatorMethod(targetType));
    }

    // 查找该枚举中符合 @JsonCreator 约定的静态 fromValue 方法
    private static Method findCreatorMethod(Class<?> enumClass) {
        return CREATOR_METHOD_CACHE.computeIfAbsent(enumClass, clazz -> {
            // 1. 先找标注 @JsonCreator 的静态方法
            Method method = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> Modifier.isStatic(m.getModifiers()))
                    .filter(m -> m.getAnnotation(com.fasterxml.jackson.annotation.JsonCreator.class) != null)
                    .findFirst()
                    .orElse(null);
            if (method != null) {
                return method;
            }
            // 2. 退而求其次：找名为 fromValue(String) / from(String) 的静态方法
            try {
                return clazz.getDeclaredMethod("fromValue", String.class);
            } catch (NoSuchMethodException e) {
                try {
                    return clazz.getDeclaredMethod("from", String.class);
                } catch (NoSuchMethodException ex) {
                    return null;
                }
            }

        });
    }

    // 实际的转换器
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
            // 如果有 creator 方法，通过反射调用
            if (creatorMethod != null) {
                try {
                    return (T) creatorMethod.invoke(null, source);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Cannot convert " + source + " to " + enumType.getSimpleName(), e);
                }
            }
            // 否则回退到 Spring 默认的 Enum.valueOf
            return (T) Enum.valueOf((Class) enumType, source);
        }
    }
}
