package tech.dhjt.boot3.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.dhjt.boot3.config.WebSocketConfig;
import tech.dhjt.boot3.event.NotificationEvent;
import tech.dhjt.boot3.model.po.Notification;
import tech.dhjt.boot3.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知服务 - 处理通知的持久化、事件监听和 WebSocket 推送
 *
 * 注意：createNotification 方法已发布 NotificationEvent 事件，
 * 而 handleNotificationEvent 监听该事件进行 WebSocket 推送，
 * 不要重复 save，因为 createNotification 已保存到数据库。
 */
@RequiredArgsConstructor
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final WebSocketConfig.NotificationWebSocketHandler webSocketHandler;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建并发送通知（保存到 DB + 发布事件推送到 WebSocket）
     */
    @Transactional
    public Notification createNotification(Long userId, String username, String title,
                                           String content, String type,
                                           String processInstanceId, String taskId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .username(username)
                .title(title)
                .content(content)
                .type(type)
                .read(false)
                .processInstanceId(processInstanceId)
                .taskId(taskId)
                .createTime(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);
        log.info("通知已保存: userId={}, title={}, type={}", userId, title, type);

        // 发布事件以触发 WebSocket 推送
        eventPublisher.publishEvent(new NotificationEvent(this, notification));

        return notification;
    }

    /**
     * 监听通知事件，推送 WebSocket（通知已在 createNotification 中持久化）
     */
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        Notification notification = event.getNotification();

        // 构建推送消息（使用 HashMap 避免 Map.of 因 null 值（如 PROCESS_END 的 taskId）抛 NPE）
        Map<String, Object> wsMessage = new HashMap<>();
        wsMessage.put("type", "notification");
        Map<String, Object> notificationBody = new HashMap<>();
        notificationBody.put("id", notification.getId());
        notificationBody.put("title", notification.getTitle());
        notificationBody.put("content", notification.getContent());
        notificationBody.put("type", notification.getType());
        if (notification.getProcessInstanceId() != null) {
            notificationBody.put("processInstanceId", notification.getProcessInstanceId());
        }
        if (notification.getTaskId() != null) {
            notificationBody.put("taskId", notification.getTaskId());
        }
        if (notification.getCreateTime() != null) {
            notificationBody.put("createTime", notification.getCreateTime().toString());
        }
        wsMessage.put("notification", notificationBody);
        wsMessage.put("unreadCount", notificationRepository.countByUserIdAndReadFalse(notification.getUserId()));

        // WebSocket 推送
        webSocketHandler.sendNotification(notification.getUserId(), wsMessage);
        log.debug("通知事件已处理: userId={}, type={}", notification.getUserId(), notification.getType());
    }

    /**
     * 获取用户未读通知
     */
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreateTimeDesc(userId);
    }

    /**
     * 获取用户所有通知
     */
    public List<Notification> getAllNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 获取未读通知数量
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            n.setReadTime(LocalDateTime.now());
            notificationRepository.save(n);
        });
    }

    /**
     * 标记用户所有通知为已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadList = notificationRepository.findByUserIdAndReadFalseOrderByCreateTimeDesc(userId);
        unreadList.forEach(n -> {
            n.setRead(true);
            n.setReadTime(LocalDateTime.now());
        });
        notificationRepository.saveAll(unreadList);
    }
}