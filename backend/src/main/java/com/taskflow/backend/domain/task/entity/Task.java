package com.taskflow.backend.domain.task.entity;

import com.taskflow.backend.domain.project.entity.Project;
import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.common.entity.BaseEntity;
import com.taskflow.backend.global.common.enums.TaskPriority;
import com.taskflow.backend.global.common.enums.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Task extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_user_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_user_id")
    private User assignee;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    private LocalDate dueDate;

    @Column(nullable = false)
    private Integer position;

    @Version
    @Column(nullable = false)
    private Long version;

    private LocalDateTime completedAt;

    private LocalDateTime deletedAt;

    public static Task create(
            Project project,
            User creator,
            User assignee,
            String title,
            String description,
            TaskPriority priority,
            LocalDate dueDate,
            int position
    ) {
        return Task.builder()
                .project(project)
                .creator(creator)
                .assignee(assignee)
                .title(title)
                .description(description)
                .status(TaskStatus.TODO)
                .priority(priority)
                .dueDate(dueDate)
                .position(position)
                .completedAt(null)
                .deletedAt(null)
                .build();
    }

    public void update(
            String title,
            String description,
            User assignee,
            TaskPriority priority,
            LocalDate dueDate
    ) {
        this.title = title;
        this.description = description;
        this.assignee = assignee;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
