package com.taskflow.backend.domain.user.repository;

import com.taskflow.backend.domain.user.entity.User;
import com.taskflow.backend.global.config.JpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail은_이메일로_사용자를_조회한다() {
        User saved = userRepository.save(User.builder()
                .email("user@example.com")
                .password("encoded")
                .nickname("tester")
                .build());

        Optional<User> found = userRepository.findByEmail("user@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void existsByEmail은_이메일_존재여부를_반환한다() {
        userRepository.save(User.builder()
                .email("exist@example.com")
                .password("encoded")
                .nickname("tester")
                .build());

        assertThat(userRepository.existsByEmail("exist@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("none@example.com")).isFalse();
    }
}
