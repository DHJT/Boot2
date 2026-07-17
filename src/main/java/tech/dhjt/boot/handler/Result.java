package tech.dhjt.boot.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * 统一 API 响应结果封装。
 * <p>
 * 所有 Controller 接口均返回此类，全局序列化为 JSON，结构统一为：
 * <pre>
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": { ... }
 * }
 * </pre>
 *
 * @param <T> data 字段的具体类型
 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /** 业务状态码 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 响应数据（成功时携带，失败时可为 null） */
    private T data;

    // ────────────────────────────── 构造器 ──────────────────────────────

    private Result() {
    }

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ────────────────────────────── 静态工厂：成功 ──────────────────────────────

    /**
     * 操作成功，无返回数据。
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    /**
     * 操作成功，带返回数据。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 操作成功，指定提示信息与返回数据。
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 操作成功，指定状态码、提示信息与返回数据。
     */
    public static <T> Result<T> success(int code, String message, T data) {
        return new Result<>(code, message, data);
    }

    // ────────────────────────────── 静态工厂：失败 ──────────────────────────────

    /**
     * 操作失败，无返回数据。
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 操作失败，带返回数据（极少场景，如校验错误详情）。
     */
    public static <T> Result<T> fail(int code, String message, T data) {
        return new Result<>(code, message, data);
    }

    // ────────────────────────────── 链式 Builder ──────────────────────────────

    public Result<T> code(int code) {
        this.code = code;
        return this;
    }

    public Result<T> message(String message) {
        this.message = message;
        return this;
    }

    public Result<T> data(T data) {
        this.data = data;
        return this;
    }

    // ────────────────────────────── 便捷判断 ──────────────────────────────

    /**
     * 是否业务成功（code == 200）。
     */
    public boolean isSuccess() {
        return this.code == 200;
    }

    // ────────────────────────────── Getter / Setter (Jackson 序列化需要) ──────────────────────────────

    // ────────────────────────────── toString ──────────────────────────────

    @Override
    public String toString() {
        return "Result{"
                + "code=" + code
                + ", message='" + message + '\''
                + ", data=" + data
                + '}';
    }
}
