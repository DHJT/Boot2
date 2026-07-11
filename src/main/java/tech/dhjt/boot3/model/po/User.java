package tech.dhjt.boot3.model.po;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.List;

/**
 * 用户实体 - 配合 Flowable IDM 进行审批绑定
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "boot_user")
@Schema(description = "用户实体")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "用户ID")
    private Long id;

    /** 用户名（登录名） */
    @Column(unique = true, nullable = false)
    @Schema(description = "用户名（登录名）")
    private String username;

    /** 显示名称 */
    @Column(nullable = false)
    @Schema(description = "显示名称")
    private String name;

    /** 邮箱 */
    @Schema(description = "邮箱")
    private String email;

    /** 密码 */
    @Schema(description = "密码")
    private String password;

    /** 手机号 */
    @Schema(description = "手机号")
    private String phone;

    /** 员工编号/工号（与示例 JSON 中的 code 对应） */
    @Schema(description = "员工编号/工号")
    private String code;

    /** 公司名称（与示例 JSON 中的 companyName 对应） */
    @Column(name = "company_name")
    @Schema(description = "公司名称")
    private String companyName;

    /** 部门ID */
    @Column(name = "dept_id")
    @Schema(description = "部门ID")
    private Long deptId;

    /** 部门名称（冗余） */
    @Column(name = "dept_name")
    @Schema(description = "部门名称")
    private String deptName;

    /** 岗位/职位 */
    @Schema(description = "岗位/职位")
    private String position;

    /** 是否启用 */
    @Builder.Default
    @Column(name = "is_enabled")
    @Schema(description = "是否启用")
    private Boolean enabled = true;

    /** Flowable 用户组ID列表（逗号分隔，如 "management,deans"） */
    @Column(name = "group_ids")
    @Schema(description = "Flowable用户组ID列表，逗号分隔")
    private String groupIds;

    /** 直接上级用户ID */
    @Column(name = "manager_id")
    @Schema(description = "直接上级用户ID")
    private Long managerId;

    /** 头像URL */
    @Column(name = "avatar_url")
    @Schema(description = "头像URL")
    private String avatarUrl;

    @Transient
    @Schema(description = "用户组列表（临时字段）")
    private List<String> groupList;

    public List<String> getGroupList() {
        if (groupIds != null && !groupIds.isEmpty()) {
            return java.util.Arrays.asList(groupIds.split(","));
        }
        return java.util.Collections.emptyList();
    }

    public void setGroupList(List<String> groupList) {
        this.groupIds = String.join(",", groupList);
    }
}
