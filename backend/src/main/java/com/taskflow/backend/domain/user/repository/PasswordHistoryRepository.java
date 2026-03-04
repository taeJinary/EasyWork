package com.taskflow.backend.domain.user.repository;

import com.taskflow.backend.domain.user.entity.PasswordHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    List<PasswordHistory> findTop3ByUserIdOrderByCreatedAtDesc(Long userId);
}

