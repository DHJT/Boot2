package tech.dhjt.boot3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.dhjt.boot3.model.po.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<Notification> findByUserIdAndReadFalseOrderByCreateTimeDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    void deleteByUserId(Long userId);
}