package tech.dhjt.boot.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.ReflectionUtils;
import tech.dhjt.boot.validation.annotation.ConditionalRequired;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ConditionalRequired} 的校验器。
 * <p>
 * 当指定条件字段的值等于期望值时，检查一个或多个目标字段是否为 null。
 * 支持 String、Enum 类型的字段值比较。
 * <p>
 * 增强特性：
 * <ul>
 *   <li>支持 {@link ConditionalRequired#targetFields()} 批量声明多个目标字段</li>
 *   <li>单个目标字段仍可使用 {@link ConditionalRequired#targetField()}，两者互斥，优先使用 targetFields</li>
 *   <li>错误消息支持 {field} / {fieldValue} / {targetField} 占位符自动替换</li>
 *   <li>批量校验时所有失败字段会一次性收集，而非短路返回</li>
 * </ul>
 */
public class ConditionalRequiredValidator implements ConstraintValidator<ConditionalRequired, Object> {

    private String field;
    private String fieldValue;
    /** 兼容旧版：单个目标字段 */
    private String targetField;
    /** 新版：多个目标字段，优先级高于 targetField */
    private String[] targetFields;
    /** 原始消息模板 */
    private String message;

    @Override
    public void initialize(ConditionalRequired annotation) {
        this.field = annotation.field();
        this.fieldValue = annotation.fieldValue();
        this.targetField = annotation.targetField();
        this.targetFields = annotation.targetFields();
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            // 1. 获取条件字段的值
            Field conditionField = ReflectionUtils.findField(value.getClass(), field);
            if (conditionField == null) {
                return true;
            }

            ReflectionUtils.makeAccessible(conditionField);
            Object actualFieldValue = ReflectionUtils.getField(conditionField, value);

            // 如果条件字段为 null，不触发校验
            if (actualFieldValue == null) {
                return true;
            }

            // 2. 比较实际字段值与期望值
            String actualStr = actualFieldValue.toString();
            if (!fieldValue.equals(actualStr)) {
                return true; // 条件不满足，不触发校验
            }

            // 3. 确定要校验的目标字段列表
            List<String> targets = resolveTargetFields();
            if (targets.isEmpty()) {
                return true;
            }

            // 4. 批量校验所有目标字段，收集所有失败
            List<String> failedFields = new ArrayList<>();
            for (String target : targets) {
                if (isFieldNull(value, target)) {
                    failedFields.add(target);
                }
            }

            // 5. 如果有失败字段，构建错误消息
            if (!failedFields.isEmpty()) {
                if (context != null) {
                    context.disableDefaultConstraintViolation();
                    for (String failedField : failedFields) {
                        String resolvedMessage = resolveMessage(failedField);
                        context.buildConstraintViolationWithTemplate(resolvedMessage)
                                .addPropertyNode(failedField)
                                .addConstraintViolation();
                    }
                }
                return false;
            }

            return true;

        } catch (Exception e) {
            // 反射异常时不阻断校验
            return true;
        }
    }

    /**
     * 解析最终的目标字段列表：优先使用 targetFields，其次使用 targetField
     */
    private List<String> resolveTargetFields() {
        List<String> result = new ArrayList<>();
        if (targetFields != null && targetFields.length > 0) {
            for (String tf : targetFields) {
                if (tf != null && !tf.isBlank()) {
                    result.add(tf.trim());
                }
            }
        }
        if (result.isEmpty() && targetField != null && !targetField.isBlank()) {
            result.add(targetField.trim());
        }
        return result;
    }

    /**
     * 检查指定字段是否为 null
     */
    private boolean isFieldNull(Object bean, String fieldName) {
        Field target = ReflectionUtils.findField(bean.getClass(), fieldName);
        if (target == null) {
            return false; // 字段不存在视作不校验
        }
        ReflectionUtils.makeAccessible(target);
        Object targetValue = ReflectionUtils.getField(target, bean);
        return targetValue == null;
    }

    /**
     * 解析消息模板，替换 {field}、{fieldValue}、{targetField} 占位符
     */
    private String resolveMessage(String actualTargetField) {
        return message
                .replace("{field}", field)
                .replace("{fieldValue}", fieldValue)
                .replace("{targetField}", actualTargetField);
    }
}