package tech.dhjt.boot.handler;

import lombok.Getter;

/**
 * 业务异常建议继承 RuntimeException。Spring 事务默认只对 RuntimeException 和 Error 回滚，
 * checked exception 默认不回滚（除非显式配置 @Transactional(rollbackFor = Exception.class)），
 * 用 RuntimeException 能省去这层心智负担。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

}
