package tech.dhjt.boot3.model.po;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "通知消息实体")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "通知ID")
    private Long id;

    /** 接收用户ID */
    @Column(name = "user_id", nullable = false)
    @Schema(description = "接收用户ID")
    private Long userId;

    /** 接收用户名 */
    @Column(name = "username")
    @Schema(description = "接收用户名")
    private String username;

    /** 标题 */
    @Column(nullable = false)
    @Schema(description = "通知标题")
    private String title;

    /** 消息内容 */
    @Column(columnDefinition = "TEXT")
    @Schema(description = "通知内容")
    private String content;

    /** 消息类型：TASK_CREATE, TASK_COMPLETE, PROCESS_END, APPROVAL_CHANGE, SYSTEM */
    @Column(name = "type", nullable = false)
    @Schema(description = "通知类型", allowableValues = {"TASK_CREATE", "TASK_COMPLETE", "PROCESS_END", "APPROVAL_CHANGE", "SYSTEM"})
    private String type;

    /** 是否已读 */
    @Builder.Default
    @Column(name = "is_read")
    @Schema(description = "是否已读")
    private Boolean read = false;

    /** 关联流程实例ID */
    @Column(name = "process_instance_id")
    @Schema(description = "关联流程实例ID")
    private String processInstanceId;

    /** 关联任务ID */
    @Column(name = "task_id")
    @Schema(description = "关联任务ID")
    private String taskId;

    /** 创建时间 */
    @Builder.Default
    @CreatedDate
    @Column(name = "create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime = LocalDateTime.now();

    /** 读取时间 */
    @Column(name = "read_time")
    @Schema(description = "读取时间")
    private LocalDateTime readTime;
}