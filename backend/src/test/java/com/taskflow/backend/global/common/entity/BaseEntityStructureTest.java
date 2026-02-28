package com.taskflow.backend.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityStructureTest {

    @Test
    void baseEntity는_감사_메타데이터_구조를_가진다() throws NoSuchFieldException {
        Class<BaseEntity> clazz = BaseEntity.class;

        assertThat(clazz.isAnnotationPresent(MappedSuperclass.class)).isTrue();
        assertThat(clazz.isAnnotationPresent(EntityListeners.class)).isTrue();
        EntityListeners entityListeners = clazz.getAnnotation(EntityListeners.class);
        assertThat(entityListeners.value()).contains(AuditingEntityListener.class);

        Field createdAt = clazz.getDeclaredField("createdAt");
        assertThat(createdAt.getType()).isEqualTo(LocalDateTime.class);
        assertThat(createdAt.isAnnotationPresent(CreatedDate.class)).isTrue();
        assertThat(createdAt.isAnnotationPresent(Column.class)).isTrue();

        Field updatedAt = clazz.getDeclaredField("updatedAt");
        assertThat(updatedAt.getType()).isEqualTo(LocalDateTime.class);
        assertThat(updatedAt.isAnnotationPresent(LastModifiedDate.class)).isTrue();
        assertThat(updatedAt.isAnnotationPresent(Column.class)).isTrue();
    }
}
