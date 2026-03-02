package com.taskflow.backend.domain.attachment.repository;

import com.taskflow.backend.domain.attachment.entity.TaskAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    List<TaskAttachment> findAllByTaskIdOrderByCreatedAtDesc(Long taskId);
}
