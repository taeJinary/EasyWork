package com.taskflow.backend.global.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TestProfileContainerAlignmentPolicyTest {

    @Test
    void applicationTestProfileDoesNotUseH2Datasource() throws IOException {
        String content = read("src/main/resources/application-test.yml");

        assertThat(content).doesNotContain("jdbc:h2:");
        assertThat(content).doesNotContain("org.h2.Driver");
        assertThat(content).doesNotContain("org.hibernate.dialect.H2Dialect");
    }

    @Test
    void applicationTestProfileUsesMysqlAndRedisEnvironmentVariables() throws IOException {
        String content = read("src/main/resources/application-test.yml");

        assertThat(content).contains("${TEST_DB_URL:");
        assertThat(content).contains("${TEST_DB_USERNAME:");
        assertThat(content).contains("${TEST_DB_PASSWORD:");
        assertThat(content).contains("${TEST_REDIS_HOST:");
        assertThat(content).contains("${TEST_REDIS_PORT:");
        assertThat(content).contains("${TEST_REDIS_PASSWORD:");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }
}
