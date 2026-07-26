package tech.dhjt.boot.enums;

import java.util.Objects;

public interface IEnum<K, V> {

    /**
     * 获取 code 值
     * @return
     */
    K getCode();

    /**
     * 获取 name 值
     * @return
     */
    V getName();

    /**
     * 获取 desc 值
     * @return
     */
    String getDesc();

    /**
     * 是否为当前枚举的默认值.
     * <p>每个枚举可以在自己的默认常量上覆写此方法返回 {@code true}，例如：</p>
     * <pre>{@code
     * UNKNOWN(0, "未知", "未知") {
     *     @Override
     *     public boolean defaultFlag() {
     *         return true;
     *     }
     * }
     * }</pre>
     *
     * @return 默认枚举常量返回 {@code true}，其余返回 {@code false}
     */
    default boolean defaultFlag() {
        return false;
    }

    /**
     * 获取默认枚举值（遍历查找 {@link #defaultFlag()} 返回 {@code true} 的常量）.
     * <p>未找到标记的默认值时，回退返回枚举的第一个常量。</p>
     * <p>例如：</p>
     * <pre>{@code
     * GenderEnum defaultEnum = IEnum.getDefault(GenderEnum.class); // 返回 GenderEnum.UNKNOWN
     * }</pre>
     *
     * @param enumClass 枚举类的 Class 对象
     * @param <K>       code 类型
     * @param <V>       desc 类型
     * @param <E>       枚举类型
     * @return 标记为默认的枚举常量；未标记时返回第一个常量；枚举为空时返回 null
     */
    static <K, V, E extends Enum<E> & IEnum<K, V>> E getDefault(Class<E> enumClass) {
        E[] constants = enumClass.getEnumConstants();
        if (constants.length == 0) {
            return null;
        }
        // 优先查找标记了 defaultFlag 的常量
        for (E constant : constants) {
            if (constant.defaultFlag()) {
                return constant;
            }
        }
        // fallback：返回第一个常量
        return constants[0];
    }

    /**
     * 根据 value（code）查找对应的枚举常量（静态工具方法）.
     * <p>适用于 {@code @JsonCreator} 标注的工厂方法中委托调用，例如：</p>
     * <pre>{@code
     * @JsonCreator
     * public static GenderEnum fromValue(int value) {
     *     return IEnum.fromValue(GenderEnum.class, value);
     * }
     * }</pre>
     *
     * @param enumClass 枚举类的 Class 对象
     * @param value     code 值
     * @param <K>       code 类型
     * @param <V>       desc 类型
     * @param <E>       枚举类型
     * @return 匹配的枚举常量，未匹配时返回 {@code null}
     */
    static <K, V, E extends Enum<E> & IEnum<K, V>> E fromValue(Class<E> enumClass, K value) {
        if (value == null) {
            return null;
        }
        for (E constant : enumClass.getEnumConstants()) {
            if (Objects.equals(constant.getCode(), value)) {
                return constant;
            }
        }
        return null;
    }

}
