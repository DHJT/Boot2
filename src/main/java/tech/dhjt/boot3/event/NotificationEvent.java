package tech.dhjt.boot3.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import tech.dhjt.boot3.model.po.Notification;

/**
 * 通知事件 - 用于 Spring Event 发布/订阅模式
 */
@Getter
public class NotificationEvent extends ApplicationEvent {

    private final Notification notification;

    public NotificationEvent(Object source, Notification notification) {
        super(source);
        this.notification = notification;
    }
}