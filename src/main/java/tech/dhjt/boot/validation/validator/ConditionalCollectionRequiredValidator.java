package tech.dhjt.boot.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.ReflectionUtils;
import tech.dhjt.boot.validation.annotation.ConditionalCollectionRequired;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * {@link ConditionalCollectionRequired} 的校验器。
 * 当指定字段的值等于期望值时，检查指定集合中每个元素的指定字段是否非空。
 * 支持 String、Enum 类型的字段值比较。
 */
public class ConditionalCollectionRequiredValidator
        implements ConstraintValidator<ConditionalCollectionRequired, Object> {

    private String field;
    private String fieldValue;
    private String collectionField;
    private String elementField;
    private String message;

    @Override
    public void initialize(ConditionalCollectionRequired annotation) {
        this.field = annotation.field();
        this.fieldValue = annotation.fieldValue();
        this.collectionField = annotation.collectionField();
        this.elementField = annotation.elementField();
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            // 获取条件字段的值
            Field conditionField = ReflectionUtils.findField(value.getClass(), field);
            if (conditionField == null) {
                return true; // 字段不存在则不校验
            }

            ReflectionUtils.makeAccessible(conditionField);
            Object actualFieldValue = ReflectionUtils.getField(conditionField, value);

            // 如果条件字段为 null，不触发校验
            if (actualFieldValue == null) {
                return true;
            }

            // 比较实际字段值与期望值
            String actualStr = actualFieldValue.toString();
            if (!fieldValue.equals(actualStr)) {
                return true; // 条件不满足，不触发校验
            }

            // 条件满足，获取集合字段
            Field collectionFieldObj = ReflectionUtils.findField(value.getClass(), collectionField);
            if (collectionFieldObj == null) {
                return true; // 集合字段不存在则不校验
            }

            ReflectionUtils.makeAccessible(collectionFieldObj);
            Object collectionValue = ReflectionUtils.getField(collectionFieldObj, value);

            // 集合为 null 或空时不触发校验（无元素无需校验）
            if (collectionValue == null) {
                return true;
            }

            if (!(collectionValue instanceof Collection<?>)) {
                return true; // 不是集合类型则不校验
            }

            Collection<?> collection = (Collection<?>) collectionValue;
            if (collection.isEmpty()) {
                return true;
            }

            // 遍历集合中的每个元素，检查目标字段是否为空
            int index = 0;
            for (Object element : collection) {
                if (element == null) {
                    index++;
                    continue;
                }

                Field targetFieldObj = ReflectionUtils.findField(element.getClass(), elementField);
                if (targetFieldObj == null) {
                    index++;
                    continue;
                }

                ReflectionUtils.makeAccessible(targetFieldObj);
                Object targetValue = ReflectionUtils.getField(targetFieldObj, element);

                // 判断是否为空（null 或空字符串）
                if (targetValue == null || (targetValue instanceof String && ((String) targetValue).trim().isEmpty())) {
                    // 校验失败，自定义错误消息
                    if (context != null) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate(
                                        String.format("%s 列表中第 %d 个元素的 %s 不能为空", collectionField, index + 1, elementField))
                                .addPropertyNode(collectionField)
                                .addConstraintViolation();
                    }
                    return false;
                }
                index++;
            }

            return true;

        } catch (Exception e) {
            return true; // 反射异常时不阻断校验
        }
    }
}