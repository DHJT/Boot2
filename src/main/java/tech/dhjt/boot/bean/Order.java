package tech.dhjt.boot.bean;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import tech.dhjt.boot.enums.OrderStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单实体
 */
@Data
//@TableName("t_order")
@TableName(value = "t_order", autoResultMap = true)
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private String customerName;

    private BigDecimal amount;

    /**
     * 订单状态（枚举类型）
     */
    private OrderStatusEnum status;

    /**
     * 下单时间（LocalDateTime）
     */
    private LocalDateTime orderTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    // 映射 JSON 字段
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraInfo;

    // 或者映射为自定义对象
    // @TableField(typeHandler = JacksonTypeHandler.class)
    // private ExtraInfo extraInfo;

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

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<OrderRelateThirdInfo> thirdInfos;

}