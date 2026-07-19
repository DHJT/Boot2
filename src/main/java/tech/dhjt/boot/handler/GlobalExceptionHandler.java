package tech.dhjt.boot.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 处理顺序很有讲究：越具体的越靠前，最后才是业务异常和兜底。
 * 顺序不是靠书写位置决定的。Spring 会自动匹配"最接近"的那个——按异常继承距离来算，离实际抛出的异常类型越近，优先级越高。
 * 所以哪怕 handleAll(Exception) 写在最前面，BizException 照样会优先命中 handleBiz。这里从具体到兜底排列，只是为了人读起来清晰。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ① 400 参数绑定 / 参数校验失败：表单对象、查询参数绑定不通过
    // 同时处理属性级校验（FieldError）和类级别校验（ObjectError，如 @ConditionalRequired）
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public Result<Void> handleValid(BindException e) {
        String msg = null;
        // 优先取属性级错误
        FieldError fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null) {
            msg = fieldError.getDefaultMessage();
        }
        // 若无属性级错误，则取类级别错误（如 @ConditionalRequired 条件校验失败）
        if (msg == null) {
            var globalError = e.getBindingResult().getGlobalError();
            if (globalError != null) {
                msg = globalError.getDefaultMessage();
            }
        }
        if (msg == null) {
            msg = "参数校验失败";
        }
        log.warn("参数校验失败：{}", msg);
        return Result.fail(400, msg);
    }

    // ② 400 路径参数 / 查询参数校验失败：@RequestParam、@PathVariable 不通过
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败：{}", msg);
        return Result.fail(400, msg);
    }

    // ③ 400 请求体解析失败：JSON 格式错误、字段类型不匹配
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return Result.fail(400, "请求参数格式错误");
    }

    // ④ 405 请求方法不支持：用 GET 调了只支持 POST 的接口
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethod(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持：{}", e.getMethod());
        return Result.fail(405, "请求方法不支持");
    }

    // ⑤ 400 非法参数：Spring 的 Assert.isTrue(...) 等校验抛出的 IllegalArgumentException
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArg(IllegalArgumentException e) {
        // 注意：该异常可能由 JDK / 第三方库抛出，message 里常含内部细节，
        // 不宜原样回显给前端，这里只记日志、对外返回固定文案
        log.warn("非法参数：{}", e.getMessage());
        return Result.fail(400, "参数不合法");
    }

    // ⑥ 业务异常：用户能看懂的提示，日志用 warn 即可
    // 注意：这里故意不加 @ResponseStatus，HTTP 状态码保持 200，由业务 code 区分
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    // ⑦ 兜底：所有没被上面接住的异常，最后都落到这里
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Result<Void> handleAll(Exception e) {
        log.error("未知异常", e);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }

}
