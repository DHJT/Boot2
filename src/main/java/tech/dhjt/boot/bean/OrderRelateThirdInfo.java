package tech.dhjt.boot.bean;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 第三方关联信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderRelateThirdInfo {

    @NotBlank(message = "第三方编号不能为空")
    private String thirdNo;

    private String info;

}
