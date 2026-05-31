package tech.dhjt.boot3.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.dhjt.boot3.model.po.Role;
import tech.dhjt.boot3.model.po.UserRole;
import tech.dhjt.boot3.repository.RoleRepository;
import tech.dhjt.boot3.repository.UserRoleRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理服务
 */
@RequiredArgsConstructor
@Service
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * 获取所有角色
     */
    public List<Role> getAllRoles() {
        return roleRepository.findByEnabledTrue();
    }

    /**
     * 根据ID获取角色
     */
    public Role getRoleById(Long id) {
        return roleRepository.findById(id).orElse(null);
    }

    /**
     * 获取用户的所有角色编码
     */
    public List<String> getUserRoleCodes(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        return userRoles.stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()).orElse(null))
                .filter(r -> r != null && r.getEnabled())
                .map(Role::getCode)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的所有角色对象
     */
    public List<Role> getUserRoles(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        return userRoles.stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()).orElse(null))
                .filter(r -> r != null && r.getEnabled())
                .collect(Collectors.toList());
    }

    /**
     * 创建角色
     */
    @Transactional
    public Role createRole(Role role) {
        Role saved = roleRepository.save(role);
        log.info("角色创建成功: id={}, name={}, code={}", saved.getId(), saved.getName(), saved.getCode());
        return saved;
    }

    /**
     * 更新角色
     */
    @Transactional
    public Role updateRole(Role role) {
        Role existing = roleRepository.findById(role.getId()).orElse(null);
        if (existing == null) {
            throw new RuntimeException("角色不存在: " + role.getId());
        }
        if (role.getName() != null) existing.setName(role.getName());
        if (role.getCode() != null) existing.setCode(role.getCode());
        if (role.getDescription() != null) existing.setDescription(role.getDescription());
        if (role.getEnabled() != null) existing.setEnabled(role.getEnabled());

        return roleRepository.save(existing);
    }

    /**
     * 删除角色
     */
    @Transactional
    public void deleteRole(Long id) {
        // 删除角色下所有用户关联
        List<UserRole> userRoles = userRoleRepository.findByRoleId(id);
        if (!userRoles.isEmpty()) {
            userRoleRepository.deleteAll(userRoles);
        }
        roleRepository.deleteById(id);
        log.info("角色已删除: id={}", id);
    }

    /**
     * 为用户分配角色
     */
    @Transactional
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        // 清除原有角色
        userRoleRepository.deleteByUserId(userId);

        // 分配新角色
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                UserRole userRole = UserRole.builder()
                        .userId(userId)
                        .roleId(roleId)
                        .build();
                userRoleRepository.save(userRole);
            }
        }
        log.info("用户 {} 角色已分配: {}", userId, roleIds);
    }
}