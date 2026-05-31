package tech.dhjt.boot3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.dhjt.boot3.model.po.UserRole;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    List<UserRole> findByRoleId(Long roleId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}