package tech.dhjt.boot3.model.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 部门实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "boot_dept")
public class Dept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 部门名称 */
    @Column(nullable = false)
    private String name;

    /** 部门编码 */
    @Column(unique = true)
    private String code;

    /** 上级部门ID */
    @Column(name = "parent_id")
    private Long parentId;

    /** 部门层级路径，如 "0/1/2" */
    @Column(name = "tree_path")
    private String treePath;

    /** 排序号 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 部门负责人 */
    @Column(name = "leader")
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 是否启用 */
    @Builder.Default
    @Column(name = "is_enabled")
    private Boolean enabled = true;

    /** 创建时间 */
    @Builder.Default
    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

}