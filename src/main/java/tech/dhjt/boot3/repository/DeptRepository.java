package tech.dhjt.boot3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.dhjt.boot3.model.po.Dept;

import java.util.List;

@Repository
public interface DeptRepository extends JpaRepository<Dept, Long> {

    List<Dept> findByParentId(Long parentId);

    List<Dept> findByEnabledTrue();

    List<Dept> findByEnabledTrueOrderBySortOrderAsc();
}