package tech.dhjt.boot3.model.po;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户-角色关联实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "boot_user_role")
@Schema(description = "用户-角色关联实体")
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "关联ID")
    private Long id;

    /** 用户ID */
    @Column(name = "user_id", nullable = false)
    @Schema(description = "用户ID")
    private Long userId;

    /** 角色ID */
    @Column(name = "role_id", nullable = false)
    @Schema(description = "角色ID")
    private Long roleId;

    /** 创建时间 */
    @Builder.Default
    @Column(name = "create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime = LocalDateTime.now();

}