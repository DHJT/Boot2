package tech.dhjt.boot.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import tech.dhjt.boot.validation.validator.ConditionalEmailValidValidator;

import java.lang.annotation.*;

/**
 * 邮箱条件校验：根据 {@code emailType} 的值校验 {@code emailInfos} 列表及其内部邮箱字段。
 * <p>
 * 规则：
 * <ul>
 *   <li>emailType = 1（固定邮箱）→ emailInfos.size() == 1，且 fixedEmails 不能为空，且内容必须符合邮箱格式</li>
 *   <li>emailType = 2（业务和客户邮箱）→ emailInfos.size() >= 1，且每个元素的 customerEmails、businessEmails 不能为空，且内容必须符合邮箱格式</li>
 *   <li>emailType = 3（订单邮箱）→ emailInfos 可以为 null 或空列表</li>
 * </ul>
 * 注：emailType 为 null 或无法匹配为有效枚举值时，不触发校验。
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ConditionalEmailValid.List.class)
@Constraint(validatedBy = ConditionalEmailValidValidator.class)
@Documented
public @interface ConditionalEmailValid {

    /**
     * @return 校验失败时的提示信息
     */
    String message() default "邮箱信息不完整，请根据邮箱类型填写正确的邮箱信息";

    /**
     * @return 分组
     */
    Class<?>[] groups() default {};

    /**
     * @return Payload
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 支持在同一类上重复使用多个 @ConditionalEmailValid
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ConditionalEmailValid[] value();
    }
}