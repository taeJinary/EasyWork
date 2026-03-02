package com.taskflow.backend.domain.attachment.entity;

import com.taskflow.backend.domain.task.entity.Task;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_user_id", nullable = false)
    private User uploader;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 255)
    private String storedFilename;

    @Column(nullable = false, length = 500)
    private String storagePath;

    @Column(length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long sizeBytes;

    public static TaskAttachment create(
            Task task,
            User uploader,
            String originalFilename,
            String storedFilename,
            String storagePath,
            String contentType,
            long sizeBytes
    ) {
        return TaskAttachment.builder()
                .task(task)
                .uploader(uploader)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .storagePath(storagePath)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .build();
    }
}
