package tech.dhjt.boot.bean.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tech.dhjt.boot.enums.OrderStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDTO {

    @NotNull
    private Long id;

    private String orderNo;

    private String customerName;

    private BigDecimal amount;


    private OrderStatusEnum status;


    private LocalDateTime orderTime;


    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer version;

}
