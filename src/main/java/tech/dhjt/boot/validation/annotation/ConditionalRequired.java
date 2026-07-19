package tech.dhjt.boot.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import tech.dhjt.boot.validation.validator.ConditionalRequiredValidator;

import java.lang.annotation.*;

/**
 * 条件必填校验：当指定字段的值等于期望值时，目标字段必须非空。
 * 支持 String 枚举比较、字段值为 null 时视为不触发条件。
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ConditionalRequired.List.class)
@Constraint(validatedBy = ConditionalRequiredValidator.class)
@Documented
public @interface ConditionalRequired {

    /**
     * @return 校验失败时的提示信息，支持 {field} / {fieldValue} / {targetField} 占位符
     */
    String message() default "当 {field} 的值为 {fieldValue} 时，{targetField} 不能为空";

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
     * @return 目标字段名（当条件满足时该字段必填），与 {@link #targetFields} 二选一，优先使用 targetFields
     */
    String targetField() default "";

    /**
     * @return 目标字段名数组（当条件满足时这些字段必填），与 {@link #targetField} 二选一<br>
     * 当设置此属性时，{@link #targetField} 将被忽略。<br>
     * 例如，当 status=PENDING 时需要校验 5 个字段，可直接声明 {@code targetFields = {"orderPersonName", "orderPersonPhone", ...}}
     */
    String[] targetFields() default {};

    /**
     * 支持在同一类上重复使用多个 @ConditionalRequired
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ConditionalRequired[] value();
    }
}