package tech.dhjt.boot.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import tech.dhjt.boot.validation.validator.ConditionalCollectionRequiredValidator;

import java.lang.annotation.*;

/**
 * 条件集合元素必填校验：当指定字段的值等于期望值时，指定集合中每个元素的指定字段必须非空。
 * 支持 String 枚举比较、字段值为 null 时视为不触发条件。
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ConditionalCollectionRequired.List.class)
@Constraint(validatedBy = ConditionalCollectionRequiredValidator.class)
@Documented
public @interface ConditionalCollectionRequired {

    /**
     * @return 校验失败时的提示信息
     */
    String message() default "当 {field} 的值为 {fieldValue} 时，{collectionField} 列表中每个元素的 {elementField} 不能为空";

    /**
     * @return 分组
     */
    Class<?>[] groups() default {};

    /**
     * @return Payload
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * @return 条件字段名（如 "status"）
     */
    String field();

    /**
     * @return 期望的字段值（如 "PROCESSING"）
     */
    String fieldValue();

    /**
     * @return 集合字段名（如 "thirdInfos"）
     */
    String collectionField();

    /**
     * @return 集合中每个元素的目标字段名（当条件满足时该字段必填）
     */
    String elementField();

    /**
     * 支持在同一类上重复使用多个 @ConditionalCollectionRequired
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ConditionalCollectionRequired[] value();
    }
}