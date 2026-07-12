package tech.dhjt.boot.bean.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import jakarta.validation.constraints.NotEmpty;
import tech.dhjt.boot.bean.OrderRelateThirdInfo;
import tech.dhjt.boot.enums.OrderStatusEnum;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class OrderVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String orderNo;

    private String customerName;

    private BigDecimal amount;

    /**
     * 订单状态（枚举类型）
     */
    private OrderStatusEnum status;

    private LocalDateTime orderTime;

    private LocalDateTime createTime;

//    @DateTimeFormat
    private LocalDateTime updateTime;

    private Integer version;

    private Map<String, Object> extraInfo;

    private List<OrderRelateThirdInfo> thirdInfos;
}
