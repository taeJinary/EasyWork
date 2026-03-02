package com.taskflow.backend.global.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationConfigSecurityPolicyTest {

    @Test
    void applicationYmlUsesEnvironmentVariablesForDatasourceCredentials() throws IOException {
        String content = read("src/main/resources/application.yml");

        assertThat(content).contains("${DB_USERNAME:");
        assertThat(content).contains("${DB_PASSWORD:");
        assertThat(content).doesNotContain("password: root1234");
    }

    @Test
    void applicationYmlDoesNotEnableVerboseSqlLoggingByDefault() throws IOException {
        String content = read("src/main/resources/application.yml");

        assertThat(content).doesNotContain("org.hibernate.SQL: DEBUG");
        assertThat(content).doesNotContain("org.hibernate.type.descriptor.sql.BasicBinder: TRACE");
    }

    @Test
    void applicationLocalYmlContainsLocalVerboseLoggingOverrides() throws IOException {
        String content = read("src/main/resources/application-local.yml");

        assertThat(content).contains("org.hibernate.SQL: DEBUG");
        assertThat(content).contains("org.hibernate.type.descriptor.sql.BasicBinder: TRACE");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }
}
