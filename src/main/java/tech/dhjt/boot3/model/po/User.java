package tech.dhjt.boot3.model.po;

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
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名（登录名） */
    @Column(unique = true, nullable = false)
    private String username;

    /** 显示名称 */
    @Column(nullable = false)
    private String name;

    /** 邮箱 */
    private String email;

    /** 密码 */
    private String password;

    /** 手机号 */
    private String phone;

    /** 部门ID */
    @Column(name = "dept_id")
    private Long deptId;

    /** 部门名称（冗余） */
    @Column(name = "dept_name")
    private String deptName;

    /** 岗位/职位 */
    private String position;

    /** 是否启用 */
    @Builder.Default
    @Column(name = "is_enabled")
    private Boolean enabled = true;

    /** Flowable 用户组ID列表（逗号分隔，如 "management,deans"） */
    @Column(name = "group_ids")
    private String groupIds;

    /** 直接上级用户ID */
    @Column(name = "manager_id")
    private Long managerId;

    /** 头像URL */
    @Column(name = "avatar_url")
    private String avatarUrl;

    @Transient
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
