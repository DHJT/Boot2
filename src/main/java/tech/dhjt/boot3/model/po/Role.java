package tech.dhjt.boot3.model.po;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "boot_role")
@Schema(description = "角色实体")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "角色ID")
    private Long id;

    /** 角色名称 */
    @Column(nullable = false)
    @Schema(description = "角色名称")
    private String name;

    /** 角色编码 */
    @Column(unique = true, nullable = false)
    @Schema(description = "角色编码")
    private String code;

    /** 角色描述 */
    @Schema(description = "角色描述")
    private String description;

    /** 是否启用 */
    @Builder.Default
    @Column(name = "is_enabled")
    @Schema(description = "是否启用")
    private Boolean enabled = true;

    /** 创建时间 */
    @Builder.Default
    @Column(name = "create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime = LocalDateTime.now();

}