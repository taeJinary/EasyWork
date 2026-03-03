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

        assertThat(content).contains("username: ${DB_USERNAME}");
        assertThat(content).contains("password: ${DB_PASSWORD}");
        assertThat(content).doesNotContain("${DB_USERNAME:root}");
        assertThat(content).doesNotContain("${DB_PASSWORD:}");
        assertThat(content).doesNotContain("password: root1234");
    }

    @Test
    void applicationYmlRequiresJwtSecretFromEnvironmentWithoutFallback() throws IOException {
        String content = read("src/main/resources/application.yml");

        assertThat(content).contains("secret: ${JWT_SECRET}");
        assertThat(content).doesNotContain("secret: ${JWT_SECRET:");
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

    @Test
    void applicationYmlDefinesMultipartFileSizeLimitsForAttachments() throws IOException {
        String content = read("src/main/resources/application.yml");

        assertThat(content).contains("multipart:");
        assertThat(content).contains("max-file-size: 10MB");
        assertThat(content).contains("max-request-size: 10MB");
    }

    @Test
    void applicationTestYmlDefinesMultipartFileSizeLimitsForAttachments() throws IOException {
        String content = read("src/main/resources/application-test.yml");

        assertThat(content).contains("multipart:");
        assertThat(content).contains("max-file-size: 10MB");
        assertThat(content).contains("max-request-size: 10MB");
    }

    @Test
    void cleanupJobMigrationSqlDefinesRequiredTableAndAuditColumns() throws IOException {
        String content = read("src/main/resources/db/migration/V20260303_01__create_task_attachment_cleanup_jobs.sql");

        assertThat(content).contains("create table if not exists task_attachment_cleanup_jobs");
        assertThat(content).contains("created_at");
        assertThat(content).contains("updated_at");
    }

    @Test
    void pushTokenMigrationSqlDefinesRequiredTableAndAuditColumns() throws IOException {
        String content = read("src/main/resources/db/migration/V20260303_02__create_notification_push_tokens.sql");

        assertThat(content).contains("create table if not exists notification_push_tokens");
        assertThat(content).contains("created_at");
        assertThat(content).contains("updated_at");
    }

    @Test
    void invitationEmailRetryMigrationSqlDefinesRequiredTableAndAuditColumns() throws IOException {
        String content = read("src/main/resources/db/migration/V20260303_03__create_invitation_email_retry_jobs.sql");

        assertThat(content).contains("create table if not exists invitation_email_retry_jobs");
        assertThat(content).contains("created_at");
        assertThat(content).contains("updated_at");
    }

    @Test
    void pushRetryMigrationSqlDefinesRequiredTableAndAuditColumns() throws IOException {
        String content = read("src/main/resources/db/migration/V20260303_04__create_notification_push_retry_jobs.sql");

        assertThat(content).contains("create table if not exists notification_push_retry_jobs");
        assertThat(content).contains("created_at");
        assertThat(content).contains("updated_at");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }
}
