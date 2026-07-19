package tech.dhjt.boot.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.constraintvalidators.AbstractEmailValidator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import tech.dhjt.boot.bean.EmailInfo;
import tech.dhjt.boot.enums.EmailTypeEnum;
import tech.dhjt.boot.validation.annotation.ConditionalEmailValid;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

/**
 * {@link ConditionalEmailValid} 的校验器。
 * <p>
 * 校验规则：
 * <ul>
 *   <li>emailType = 1（固定邮箱）→ emailInfos.size() == 1，且 {@link EmailInfo#fixedEmails} 不能为空，且内容必须符合邮箱格式</li>
 *   <li>emailType = 2（业务和客户邮箱）→ emailInfos.size() >= 1，且每个 {@link EmailInfo} 的 customerEmails、businessEmails 不能为空，且内容必须符合邮箱格式</li>
 *   <li>emailType = 3（订单邮箱）→ emailInfos 可以为 null 或空列表</li>
 * </ul>
 * 当 emailType 为 null 或无法匹配时，不触发校验。
 */
public class ConditionalEmailValidValidator implements ConstraintValidator<ConditionalEmailValid, Object> {

    private static final AbstractEmailValidator<Annotation> EMAIL_VALIDATOR = new AbstractEmailValidator<>();

    private String message;

    @Override
    public void initialize(ConditionalEmailValid annotation) {
        this.message = annotation.message();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            // 获取 emailType 字段
            Field emailTypeField = ReflectionUtils.findField(value.getClass(), "emailType");
            if (emailTypeField == null) {
                return true;
            }
            ReflectionUtils.makeAccessible(emailTypeField);
            Object emailTypeValue = ReflectionUtils.getField(emailTypeField, value);

            // emailType 为 null 时不触发校验
            if (emailTypeValue == null) {
                return true;
            }

            // 转换为 EmailTypeEnum
            EmailTypeEnum emailType;
            if (emailTypeValue instanceof EmailTypeEnum) {
                emailType = (EmailTypeEnum) emailTypeValue;
            } else {
                // 如果是 Integer 或其他类型，尝试转换
                try {
                    int code = Integer.parseInt(emailTypeValue.toString());
                    emailType = EmailTypeEnum.fromValue(code);
                } catch (NumberFormatException e) {
                    return true;
                }
            }

            // 如果是 UNKNOWN 则不校验
            if (emailType == EmailTypeEnum.UNKNOWN) {
                return true;
            }

            // 获取 emailInfos 字段（注意：OrderDTO 中是 emailInfos，复数 List）
            Field emailInfosField = ReflectionUtils.findField(value.getClass(), "emailInfos");
            if (emailInfosField == null) {
                return true;
            }
            ReflectionUtils.makeAccessible(emailInfosField);
            List<EmailInfo> emailInfos = (List<EmailInfo>) ReflectionUtils.getField(emailInfosField, value);

            switch (emailType) {
                case FIXED_EMAIL:
                    // emailType=1：emailInfos.size() == 1
                    if (emailInfos == null || emailInfos.size() != 1) {
                        addConstraintViolation(context, "当邮箱类型为固定邮箱时，emailInfos 数量必须为 1");
                        return false;
                    }
                    return validateEmailInfo(context, emailInfos.get(0), true, false, false);

                case BUSINESS_CUSTOMER_EMAIL:
                    // emailType=2：emailInfos.size() >= 1
                    if (emailInfos == null || emailInfos.isEmpty()) {
                        addConstraintViolation(context, "当邮箱类型为业务和客户邮箱时，emailInfos 数量不能为空");
                        return false;
                    }
                    for (EmailInfo emailInfo : emailInfos) {
                        if (!validateEmailInfo(context, emailInfo, false, true, true)) {
                            return false;
                        }
                    }
                    return true;

                case ORDER_EMAIL:
                    // emailType=3：emailInfos 为 null 或空列表都允许
                    return true;

                default:
                    return true;
            }

        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 校验单个 EmailInfo 中各邮箱列表的内容格式
     *
     * @param context             校验上下文
     * @param emailInfo           待校验的邮箱信息
     * @param checkFixed          是否校验 fixedEmails
     * @param checkCustomer       是否校验 customerEmails
     * @param checkBusiness       是否校验 businessEmails
     * @return true 通过，false 不通过
     */
    private boolean validateEmailInfo(ConstraintValidatorContext context,
                                      EmailInfo emailInfo,
                                      boolean checkFixed,
                                      boolean checkCustomer,
                                      boolean checkBusiness) {
        if (emailInfo == null) {
            return false;
        }

        // 校验 fixedEmails
        if (checkFixed) {
            if (CollectionUtils.isEmpty(emailInfo.getFixedEmails())) {
                addConstraintViolation(context, "fixedEmails 不能为空");
                return false;
            }
            if (!allMatchEmailFormat(emailInfo.getFixedEmails())) {
                addConstraintViolation(context, "fixedEmails 中存在不符合邮箱格式的内容");
                return false;
            }
        }

        // 校验 customerEmails
        if (checkCustomer) {
            if (CollectionUtils.isEmpty(emailInfo.getCustomerEmails())) {
                addConstraintViolation(context, "customerEmails 不能为空");
                return false;
            }
            if (!allMatchEmailFormat(emailInfo.getCustomerEmails())) {
                addConstraintViolation(context, "customerEmails 中存在不符合邮箱格式的内容");
                return false;
            }
        }

        // 校验 businessEmails
        if (checkBusiness) {
            if (CollectionUtils.isEmpty(emailInfo.getBusinessEmails())) {
                addConstraintViolation(context, "businessEmails 不能为空");
                return false;
            }
            if (!allMatchEmailFormat(emailInfo.getBusinessEmails())) {
                addConstraintViolation(context, "businessEmails 中存在不符合邮箱格式的内容");
                return false;
            }
        }

        return true;
    }

    /**
     * 校验列表中所有字符串是否符合邮箱格式
     */
    private boolean allMatchEmailFormat(List<String> emails) {
        if (emails == null) {
            return true;
        }
        for (String email : emails) {
            if (email == null || !EMAIL_VALIDATOR.isValid(email, null)) {
                return false;
            }
        }
        return true;
    }

    private void addConstraintViolation(ConstraintValidatorContext context, String customMessage) {
        if (context != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(customMessage)
                    .addPropertyNode("emailInfos")
                    .addConstraintViolation();
        }
    }
}