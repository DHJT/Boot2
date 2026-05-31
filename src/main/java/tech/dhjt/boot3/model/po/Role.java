package tech.dhjt.boot3.model.po;

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
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 角色名称 */
    @Column(nullable = false)
    private String name;

    /** 角色编码 */
    @Column(unique = true, nullable = false)
    private String code;

    /** 角色描述 */
    private String description;

    /** 是否启用 */
    @Builder.Default
    @Column(name = "is_enabled")
    private Boolean enabled = true;

    /** 创建时间 */
    @Builder.Default
    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

}