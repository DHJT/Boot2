package tech.dhjt.boot3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.dhjt.boot3.model.po.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByName(String name);

//    Optional<User> findByName1(String name);

    List<User> findByDeptId(Long deptId);

    List<User> findByManagerId(Long managerId);

    List<User> findByGroupIdsContaining(String groupId);

    List<User> findByEnabledTrue();
}