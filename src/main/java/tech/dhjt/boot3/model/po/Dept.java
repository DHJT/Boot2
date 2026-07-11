package tech.dhjt.boot3.model.po;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "部门实体")
public class Dept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "部门ID")
    private Long id;

    /** 部门名称 */
    @Column(nullable = false)
    @Schema(description = "部门名称")
    private String name;

    /** 部门编码 */
    @Column(unique = true)
    @Schema(description = "部门编码")
    private String code;

    /** 上级部门ID */
    @Column(name = "parent_id")
    @Schema(description = "上级部门ID")
    private Long parentId;

    /** 部门层级路径，如 "0/1/2" */
    @Column(name = "tree_path")
    @Schema(description = "部门层级路径，如 0/1/2")
    private String treePath;

    /** 排序号 */
    @Column(name = "sort_order")
    @Schema(description = "排序号")
    private Integer sortOrder;

    /** 部门负责人 */
    @Column(name = "leader")
    @Schema(description = "部门负责人")
    private String leader;

    /** 联系电话 */
    @Schema(description = "联系电话")
    private String phone;

    /** 邮箱 */
    @Schema(description = "邮箱")
    private String email;

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