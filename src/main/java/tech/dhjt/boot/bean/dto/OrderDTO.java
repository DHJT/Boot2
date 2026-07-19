package tech.dhjt.boot.bean.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;
import org.hibernate.validator.constraints.CreditCardNumber;
import tech.dhjt.boot.bean.EmailInfo;
import tech.dhjt.boot.bean.OrderRelateThirdInfo;
import tech.dhjt.boot.enums.EmailTypeEnum;
import tech.dhjt.boot.enums.OrderStatusEnum;
import tech.dhjt.boot.validation.annotation.ConditionalCollectionRequired;
import tech.dhjt.boot.validation.annotation.ConditionalEmailValid;
import tech.dhjt.boot.validation.annotation.ConditionalRequired;
import tech.dhjt.boot.validation.groups.Create;
import tech.dhjt.boot.validation.groups.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 DTO
 * <p>
 * 校验规则：<br>
 * - Create：orderNo / customerName / amount 必填，id 为 null<br>
 * - Update：id 必填，orderNo / customerName / amount 按需更新<br>
 * - 条件校验：当 status=PROCESSING 时 customerName 必填<br>
 * - 邮箱校验：根据 emailType 校验 emailInfos 数量及内部邮箱格式（由 @ConditionalEmailValid 处理）<br>
 * - 嵌套校验：thirdInfos 列表中的对象也需校验
 */
@Data
@ConditionalCollectionRequired.List({
        @ConditionalCollectionRequired(
                field = "status",
                fieldValue = "PROCESSING",
                collectionField = "thirdInfos",
                elementField = "info",
                message = "当订单状态为 PROCESSING 时，thirdInfos 列表中每个元素的 info 不能为空",
                groups = {Create.class, Update.class}
        )
})
@ConditionalRequired.List({
        @ConditionalRequired(
                field = "status",
                fieldValue = "PROCESSING",
                targetField = "customerName",
                message = "当订单状态为 PROCESSING 时，customerName 不能为空",
                groups = {Create.class, Update.class}
        ),
        @ConditionalRequired(
                field = "status",
                fieldValue = "COMPLETED",
                targetFields = "orderTime",
                message = "当订单状态为 COMPLETED 时，orderTime 不能为空",
                groups = {Create.class, Update.class}
        ),
        // ========== PENDING 状态：下单人信息必填（5个字段合并为单个注解） ==========
        @ConditionalRequired(
                field = "status",
                fieldValue = "PENDING",
                targetFields = {
                        "orderPersonName",
                        "orderPersonPhone",
                        "orderPersonAddress",
                        "orderPersonBank",
                        "orderPersonBankBranch"
                },
                message = "当订单状态为 PENDING 时，{targetField} 不能为空",
                groups = {Create.class, Update.class}
        )
})
@ConditionalEmailValid(groups = {Create.class, Update.class})
public class OrderDTO {

    // ========== Create 分组：新增时 id 必须为 null（由数据库自动生成） ==========
    @Null(groups = Create.class, message = "新增时 id 必须为 null")
    // ========== Update 分组：更新时 id 必须填写 ==========
    @NotNull(groups = Update.class, message = "更新时 id 不能为空")
    private Long id;

    // ========== Create 分组：新增时必须填写 ==========
    @NotBlank(groups = Create.class, message = "订单编号不能为空")
    // ========== Update 分组：更新时至少保留原值，不强制校验 ==========
    private String orderNo;

    // ========== Create 分组：新增时必须填写 ==========
    @NotBlank(groups = Create.class, message = "客户名称不能为空")
    private String customerName;

    // ========== Create 分组：新增时必须填写 ==========
    @NotNull(groups = Create.class, message = "订单金额不能为空")
    @DecimalMin(value = "0.01", groups = {Create.class, Update.class}, message = "订单金额必须大于 0")
    private BigDecimal amount;

    // 状态：新增时由后端默认 PENDING，更新时不修改；均不校验入参
    private OrderStatusEnum status;

    // 下单时间：更新时可选
    private LocalDateTime orderTime;

    // 系统填充字段，禁止手动传入
    @Null(groups = {Create.class, Update.class}, message = "创建时间由系统自动填充")
    private LocalDateTime createTime;

    @Null(groups = {Create.class, Update.class}, message = "更新时间由系统自动填充")
    private LocalDateTime updateTime;

    // 乐观锁版本号：更新时必须传入
    @NotNull(groups = Update.class, message = "更新时版本号不能为空")
    private Integer version;

    // 邮箱类型：1-固定邮箱 2-业务和客户邮箱 3-订单邮箱
    private EmailTypeEnum emailType;

    // 邮箱信息（根据 emailType 决定哪些字段必须填写）
    @Valid
    private List<EmailInfo> emailInfos;

    // ========== 下单人信息（PENDING 状态时必填） ==========

    /** 下单人名称 */
    private String orderPersonName;

    /** 下单人联系方式 */
    private String orderPersonPhone;

    /** 下单人地址 */
    private String orderPersonAddress;

    /** 下单人银行 */
    @CreditCardNumber
    private String orderPersonBank;

    /** 下单人开户行 */
    private String orderPersonBankBranch;

    // 嵌套校验第三方关联信息
    @Valid
    private List<OrderRelateThirdInfo> thirdInfos;
}
