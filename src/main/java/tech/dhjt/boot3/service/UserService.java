package tech.dhjt.boot3.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.flowable.idm.api.IdmIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.dhjt.boot3.config.JwtUtil;
import tech.dhjt.boot3.model.po.User;
import tech.dhjt.boot3.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户服务 - JWT 登录、用户/部门查询/CRUD
 */
@RequiredArgsConstructor
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RoleService roleService;
    private final IdmIdentityService idmIdentityService;

    /**
     * 初始化用户数据（首次启动）并同步已存在的用户到 Flowable IDM
     */
    @PostConstruct
    @Transactional
    public void initDemoUsers() {
        if (userRepository.count() > 0) {
            // 数据库已有数据，但也要确保这些用户已同步到 Flowable IDM
            log.info("用户数据已存在，同步到 Flowable IDM...");
            try {
                syncUsersToIdm();
            } catch (Exception e) {
                log.warn("Flowable IDM 同步失败 (可能 IDM 未启用): {}", e.getMessage());
            }
            return;
        }

        // 管理员
        userRepository.save(User.builder()
                .username("admin").name("管理员").password("123456")
                .email("admin@dhjt.tech").code("10001").companyName("中国石化").deptId(1L).deptName("技术部")
                .position("系统管理员").groupIds("admin").enabled(true)
                .build());

        // 张三 - 辅导员审批人之一
        userRepository.save(User.builder()
                .username("zhangsan").name("张三").password("123456")
                .email("zhangsan@dhjt.tech").code("10002").companyName("中国石化").deptId(1L).deptName("技术部")
                .position("辅导员").groupIds("employee,advisor").managerId(2L).enabled(true)
                .build());

        // 李四 - 辅导员审批人之一
        userRepository.save(User.builder()
                .username("lisi").name("李四").password("123456")
                .email("lisi@dhjt.tech").code("10003").companyName("中国石化").deptId(1L).deptName("技术部")
                .position("辅导员").groupIds("management,advisor").managerId(3L).enabled(true)
                .build());

        // 王五 - 总监
        userRepository.save(User.builder()
                .username("wangwu").name("王五").password("123456")
                .email("wangwu@dhjt.tech").code("10004").companyName("中国石化").deptId(2L).deptName("管理部")
                .position("总监").groupIds("directors,dean").managerId(4L).enabled(true)
                .build());

        // 赵六 - HR
        userRepository.save(User.builder()
                .username("zhaoliu").name("赵六").password("123456")
                .email("zhaoliu@dhjt.tech").code("10005").companyName("中国石化").deptId(3L).deptName("人力资源部")
                .position("HR经理").groupIds("hr").managerId(3L).enabled(true)
                .build());

        // 孙七 - 辅导员
        userRepository.save(User.builder()
                .username("sunqi").name("孙七").password("123456")
                .email("sunqi@dhjt.tech").code("10006").companyName("中国石化").deptId(4L).deptName("学生处")
                .position("辅导员").groupIds("advisor").managerId(3L).enabled(true)
                .build());

        // 周八 - 院长
        userRepository.save(User.builder()
                .username("zhouba").name("周八").password("123456")
                .email("zhouba@dhjt.tech").code("10007").companyName("中国石化").deptId(5L).deptName("院办")
                .position("院长").groupIds("dean,management").enabled(true)
                .build());

        log.info("演示用户数据初始化完成（共 7 个用户）");

        // 将所有用户同步到 Flowable IDM
        syncUsersToIdm();
    }

    /**
     * 将用户数据同步到 Flowable IDM 引擎
     */
    private void syncUsersToIdm() {
        List<User> allUsers = userRepository.findAll();
        log.info("开始同步 {} 个用户到 Flowable IDM...", allUsers.size());
        for (User user : allUsers) {
            syncSingleUserToIdm(user);
        }
        log.info("Flowable IDM 用户同步完成");
    }

    /**
     * 用户登录 - 返回 JWT Token
     */
    public Map<String, Object> login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOpt.get();
        if (!user.getEnabled()) {
            throw new RuntimeException("用户已被禁用");
        }

        // 简单密码校验（演示用）
        if (!"123456".equals(password)) {
            throw new RuntimeException("密码错误");
        }

        // 获取用户角色
        List<String> roles = roleService.getUserRoleCodes(user.getId());
        // 如果没有角色，默认给一个 USER 角色
        if (roles.isEmpty()) {
            roles.add("ROLE_USER");
        }

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getName(), roles);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("name", user.getName());
        result.put("email", user.getEmail());
        result.put("code", user.getCode());
        result.put("companyName", user.getCompanyName());
        result.put("deptId", user.getDeptId());
        result.put("deptName", user.getDeptName());
        result.put("position", user.getPosition());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("groups", user.getGroupList());
        result.put("roles", roles);

        log.info("用户 {} 登录成功，生成 JWT Token", username);
        return result;
    }

    /**
     * 获取所有用户列表
     */
    public List<User> getAllUsers() {
        return userRepository.findByEnabledTrue();
    }

    /**
     * 根据用户组ID查找用户
     */
    public List<User> getUsersByGroup(String groupId) {
        return userRepository.findByGroupIdsContaining(groupId);
    }

    /**
     * 根据部门ID查找用户
     */
    public List<User> getUsersByDept(Long deptId) {
        return userRepository.findByDeptId(deptId);
    }

    /**
     * 根据用户ID查找
     */
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /**
     * 根据用户名查找
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * 创建用户（同时同步到 Flowable IDM）
     */
    @Transactional
    public User createUser(User user) {
        if (user.getEnabled() == null) {
            user.setEnabled(true);
        }
        User saved = userRepository.save(user);
        log.info("用户创建成功: id={}, username={}", saved.getId(), saved.getUsername());
        // 同步到 Flowable IDM
        syncSingleUserToIdm(saved);
        return saved;
    }

    /**
     * 更新用户（同时同步到 Flowable IDM）
     */
    @Transactional
    public User updateUser(User user) {
        User existing = userRepository.findById(user.getId()).orElse(null);
        if (existing == null) {
            throw new RuntimeException("用户不存在: " + user.getId());
        }
        if (user.getUsername() != null) existing.setUsername(user.getUsername());
        if (user.getName() != null) existing.setName(user.getName());
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getPhone() != null) existing.setPhone(user.getPhone());
        if (user.getCode() != null) existing.setCode(user.getCode());
        if (user.getCompanyName() != null) existing.setCompanyName(user.getCompanyName());
        if (user.getDeptId() != null) existing.setDeptId(user.getDeptId());
        if (user.getDeptName() != null) existing.setDeptName(user.getDeptName());
        if (user.getPosition() != null) existing.setPosition(user.getPosition());
        if (user.getEnabled() != null) existing.setEnabled(user.getEnabled());
        if (user.getGroupIds() != null) existing.setGroupIds(user.getGroupIds());
        if (user.getManagerId() != null) existing.setManagerId(user.getManagerId());

        User updated = userRepository.save(existing);
        // 同步到 Flowable IDM
        syncSingleUserToIdm(updated);
        return updated;
    }

    /**
     * 删除用户（同时从 Flowable IDM 删除）
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            try {
                org.flowable.idm.api.User idmUser = idmIdentityService.createUserQuery().userId(user.getUsername()).singleResult();
                if (idmUser != null) {
                    idmIdentityService.deleteUser(idmUser.getId());
                    log.debug("已从 Flowable IDM 删除用户: {}", user.getUsername());
                }
            } catch (Exception e) {
                log.warn("从 Flowable IDM 删除用户失败: {}", e.getMessage());
            }
        }
        userRepository.deleteById(id);
        log.info("用户已删除: id={}", id);
    }

    /**
     * 将单个用户同步到 Flowable IDM
     */
    private void syncSingleUserToIdm(User user) {
        try {
            org.flowable.idm.api.User idmUser = idmIdentityService.createUserQuery().userId(user.getUsername()).singleResult();
            boolean isNew = false;
            if (idmUser == null) {
                idmUser = idmIdentityService.newUser(user.getUsername());
                isNew = true;
            }
            idmUser.setDisplayName(user.getName());
            idmUser.setFirstName(user.getName());
            idmUser.setLastName(user.getName());
            idmUser.setEmail(user.getEmail());
            idmUser.setPassword(user.getPassword());
            idmIdentityService.saveUser(idmUser);
            log.debug("已{} Flowable IDM 用户: {}", isNew ? "同步" : "更新", user.getUsername());
        } catch (Exception e) {
            log.warn("同步用户 {} 到 Flowable IDM 失败: {}", user.getUsername(), e.getMessage());
        }
    }
}