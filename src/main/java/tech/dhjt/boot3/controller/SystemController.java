package tech.dhjt.boot3.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tech.dhjt.boot3.model.po.Dept;
import tech.dhjt.boot3.model.po.Role;
import tech.dhjt.boot3.model.po.User;
import tech.dhjt.boot3.service.DeptService;
import tech.dhjt.boot3.service.RoleService;
import tech.dhjt.boot3.service.UserService;

import java.util.List;

/**
 * 系统管理 REST API — 部门/角色/用户管理
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/system")
@Tag(name = "系统管理", description = "部门、角色、用户的管理接口")
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);

    private final DeptService deptService;
    private final RoleService roleService;
    private final UserService userService;

    // =====================================================================
    //  部门管理
    // =====================================================================

    @Operation(summary = "查询所有部门列表")
    @GetMapping("/depts")
    public List<Dept> getDepts() {
        return deptService.getAllDepts();
    }

    @Operation(summary = "根据ID查询部门")
    @GetMapping("/depts/{id}")
    public Dept getDept(@Parameter(description = "部门ID") @PathVariable Long id) {
        return deptService.getDeptById(id);
    }

    @Operation(summary = "创建部门")
    @PostMapping("/depts")
    public Dept createDept(@RequestBody Dept dept) {
        return deptService.createDept(dept);
    }

    @Operation(summary = "更新部门")
    @PutMapping("/depts")
    public Dept updateDept(@RequestBody Dept dept) {
        return deptService.updateDept(dept);
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/depts/{id}")
    public String deleteDept(@Parameter(description = "部门ID") @PathVariable Long id) {
        deptService.deleteDept(id);
        return "部门已删除";
    }

    // =====================================================================
    //  角色管理
    // =====================================================================

    @Operation(summary = "查询所有角色列表")
    @GetMapping("/roles")
    public List<Role> getRoles() {
        return roleService.getAllRoles();
    }

    @Operation(summary = "根据ID查询角色")
    @GetMapping("/roles/{id}")
    public Role getRole(@Parameter(description = "角色ID") @PathVariable Long id) {
        return roleService.getRoleById(id);
    }

    @Operation(summary = "创建角色")
    @PostMapping("/roles")
    public Role createRole(@RequestBody Role role) {
        return roleService.createRole(role);
    }

    @Operation(summary = "更新角色")
    @PutMapping("/roles")
    public Role updateRole(@RequestBody Role role) {
        return roleService.updateRole(role);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/roles/{id}")
    public String deleteRole(@Parameter(description = "角色ID") @PathVariable Long id) {
        roleService.deleteRole(id);
        return "角色已删除";
    }

    @Operation(summary = "获取用户角色", description = "查询指定用户的所有角色")
    @GetMapping("/users/{userId}/roles")
    public List<Role> getUserRoles(@Parameter(description = "用户ID") @PathVariable Long userId) {
        return roleService.getUserRoles(userId);
    }

    @Operation(summary = "分配用户角色", description = "为用户分配多个角色")
    @PostMapping("/users/{userId}/roles")
    public String assignUserRoles(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "角色ID列表") @RequestBody List<Long> roleIds) {
        roleService.assignRolesToUser(userId, roleIds);
        return "角色分配成功";
    }

    // =====================================================================
    //  用户管理
    // =====================================================================

    @Operation(summary = "查询所有用户列表")
    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    @Operation(summary = "根据ID查询用户")
    @GetMapping("/users/{id}")
    public User getUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        return userService.getUserById(id);
    }

    @Operation(summary = "创建用户")
    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/users")
    public User updateUser(@RequestBody User user) {
        return userService.updateUser(user);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/users/{id}")
    public String deleteUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        userService.deleteUser(id);
        return "用户已删除";
    }

    @Operation(summary = "按部门查询用户")
    @GetMapping("/users/dept/{deptId}")
    public List<User> getUsersByDept(@Parameter(description = "部门ID") @PathVariable Long deptId) {
        return userService.getUsersByDept(deptId);
    }

    // =====================================================================
    //  系统初始化数据
    // =====================================================================

    /**
     * 初始化演示部门
     */
    @PostConstruct
    @Transactional
    public void initDemoData() {
        // 部门已存在则不初始化
        if (deptService.getDeptById(1L) != null) {
            return;
        }

        log.info("初始化演示部门数据...");

        // 创建部门
        deptService.createDept(Dept.builder().id(1L).name("技术部").code("tech").parentId(0L).treePath("0").sortOrder(1).leader("李四").build());
        deptService.createDept(Dept.builder().id(2L).name("管理部").code("management").parentId(0L).treePath("0").sortOrder(2).leader("王五").build());
        deptService.createDept(Dept.builder().id(3L).name("人力资源部").code("hr").parentId(0L).treePath("0").sortOrder(3).leader("赵六").build());
        deptService.createDept(Dept.builder().id(4L).name("学生处").code("student").parentId(0L).treePath("0").sortOrder(4).leader("孙七").build());
        deptService.createDept(Dept.builder().id(5L).name("院办").code("dean").parentId(0L).treePath("0").sortOrder(5).leader("周八").build());

        // 创建角色
        log.info("初始化演示角色数据...");
        roleService.createRole(Role.builder().id(1L).name("系统管理员").code("ROLE_ADMIN").description("系统管理员，拥有所有权限").build());
        roleService.createRole(Role.builder().id(2L).name("普通用户").code("ROLE_USER").description("普通用户，可以发起流程").build());
        roleService.createRole(Role.builder().id(3L).name("部门经理").code("ROLE_MANAGER").description("部门经理，可以审批部门内申请").build());
        roleService.createRole(Role.builder().id(4L).name("总监").code("ROLE_DIRECTOR").description("总监，可以审批跨部门申请").build());
        roleService.createRole(Role.builder().id(5L).name("院长").code("ROLE_DEAN").description("院长，拥有最终审批权").build());

        // 为用户分配角色（依赖于 UserService @PostConstruct 先执行）
        log.info("为用户分配演示角色...");
        User admin = userService.getUserByUsername("admin");
        if (admin != null) roleService.assignRolesToUser(admin.getId(), List.of(1L, 2L));

        User zhangsan = userService.getUserByUsername("zhangsan");
        if (zhangsan != null) roleService.assignRolesToUser(zhangsan.getId(), List.of(2L));

        User lisi = userService.getUserByUsername("lisi");
        if (lisi != null) roleService.assignRolesToUser(lisi.getId(), List.of(2L, 3L));

        User wangwu = userService.getUserByUsername("wangwu");
        if (wangwu != null) roleService.assignRolesToUser(wangwu.getId(), List.of(2L, 4L));

        User zhaoliu = userService.getUserByUsername("zhaoliu");
        if (zhaoliu != null) roleService.assignRolesToUser(zhaoliu.getId(), List.of(2L, 3L));

        User sunqi = userService.getUserByUsername("sunqi");
        if (sunqi != null) roleService.assignRolesToUser(sunqi.getId(), List.of(2L));

        User zhouba = userService.getUserByUsername("zhouba");
        if (zhouba != null) roleService.assignRolesToUser(zhouba.getId(), List.of(2L, 5L));

        log.info("演示数据初始化完成");
    }
}