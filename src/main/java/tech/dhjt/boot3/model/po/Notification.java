package tech.dhjt.boot3.model.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

/**
 * 通知消息记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "boot_notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 接收用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 接收用户名 */
    @Column(name = "username")
    private String username;

    /** 标题 */
    @Column(nullable = false)
    private String title;

    /** 消息内容 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 消息类型：TASK_CREATE, TASK_COMPLETE, PROCESS_END, APPROVAL_CHANGE, SYSTEM */
    @Column(name = "type", nullable = false)
    private String type;

    /** 是否已读 */
    @Builder.Default
    @Column(name = "is_read")
    private Boolean read = false;

    /** 关联流程实例ID */
    @Column(name = "process_instance_id")
    private String processInstanceId;

    /** 关联任务ID */
    @Column(name = "task_id")
    private String taskId;

    /** 创建时间 */
    @Builder.Default
    @CreatedDate
    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    /** 读取时间 */
    @Column(name = "read_time")
    private LocalDateTime readTime;
}