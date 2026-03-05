package com.taskflow.backend.global.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveValueSanitizerTest {

    @Test
    void shortHashReturnsEmptyWhenValueIsBlank() {
        assertThat(SensitiveValueSanitizer.shortHash(null)).isEqualTo("empty");
        assertThat(SensitiveValueSanitizer.shortHash("")).isEqualTo("empty");
        assertThat(SensitiveValueSanitizer.shortHash("   ")).isEqualTo("empty");
    }

    @Test
    void shortHashReturnsDeterministicMaskedValue() {
        String raw = "task-attachments/10/abc-report.pdf";
        String hash1 = SensitiveValueSanitizer.shortHash(raw);
        String hash2 = SensitiveValueSanitizer.shortHash(raw);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(16);
        assertThat(hash1).doesNotContain("task-attachments");
        assertThat(hash1).doesNotContain("abc-report");
    }
}
