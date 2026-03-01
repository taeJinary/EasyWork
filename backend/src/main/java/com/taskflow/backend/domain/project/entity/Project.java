package com.taskflow.backend.domain.project.entity;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "projects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String description;

    private LocalDateTime deletedAt;

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void touch(LocalDateTime changedAt) {
        touchUpdatedAt(changedAt);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}

