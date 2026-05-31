package tech.dhjt.boot3.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.dhjt.boot3.model.po.Dept;
import tech.dhjt.boot3.repository.DeptRepository;

import java.util.List;

/**
 * 部门管理服务
 */
@RequiredArgsConstructor
@Service
public class DeptService {

    private static final Logger log = LoggerFactory.getLogger(DeptService.class);

    private final DeptRepository deptRepository;

    /**
     * 获取所有部门（树形展示全部）
     */
    public List<Dept> getAllDepts() {
        return deptRepository.findByEnabledTrueOrderBySortOrderAsc();
    }

    /**
     * 根据ID获取部门
     */
    public Dept getDeptById(Long id) {
        return deptRepository.findById(id).orElse(null);
    }

    /**
     * 获取子部门列表
     */
    public List<Dept> getChildDepts(Long parentId) {
        return deptRepository.findByParentId(parentId);
    }

    /**
     * 创建部门
     */
    @Transactional
    public Dept createDept(Dept dept) {
        // 计算 treePath
        if (dept.getParentId() != null && dept.getParentId() > 0) {
            Dept parent = deptRepository.findById(dept.getParentId()).orElse(null);
            if (parent != null) {
                dept.setTreePath((parent.getTreePath() != null ? parent.getTreePath() : "0") + "/" + parent.getId());
            }
        } else {
            dept.setParentId(0L);
            dept.setTreePath("0");
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }
        Dept saved = deptRepository.save(dept);
        log.info("部门创建成功: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    /**
     * 更新部门
     */
    @Transactional
    public Dept updateDept(Dept dept) {
        Dept existing = deptRepository.findById(dept.getId()).orElse(null);
        if (existing == null) {
            throw new RuntimeException("部门不存在: " + dept.getId());
        }
        if (dept.getName() != null) existing.setName(dept.getName());
        if (dept.getCode() != null) existing.setCode(dept.getCode());
        if (dept.getParentId() != null) existing.setParentId(dept.getParentId());
        if (dept.getLeader() != null) existing.setLeader(dept.getLeader());
        if (dept.getPhone() != null) existing.setPhone(dept.getPhone());
        if (dept.getEmail() != null) existing.setEmail(dept.getEmail());
        if (dept.getSortOrder() != null) existing.setSortOrder(dept.getSortOrder());
        if (dept.getEnabled() != null) existing.setEnabled(dept.getEnabled());

        return deptRepository.save(existing);
    }

    /**
     * 删除部门
     */
    @Transactional
    public void deleteDept(Long id) {
        // 检查是否有子部门
        List<Dept> children = deptRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new RuntimeException("该部门下有子部门，无法删除");
        }
        deptRepository.deleteById(id);
        log.info("部门已删除: id={}", id);
    }
}