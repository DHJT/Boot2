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

    /** 下单人名称 */
    private String orderPersonName;

    /** 下单人联系方式 */
    private String orderPersonPhone;

    /** 下单人地址 */
    private String orderPersonAddress;

    /** 下单人银行 */
    private String orderPersonBank;

    /** 下单人开户行 */
    private String orderPersonBankBranch;

    private Map<String, Object> extraInfo;

    private List<OrderRelateThirdInfo> thirdInfos;
}
