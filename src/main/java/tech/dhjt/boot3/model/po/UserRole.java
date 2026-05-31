package tech.dhjt.boot3.model.po;

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
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 角色ID */
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /** 创建时间 */
    @Builder.Default
    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

}