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
    void applicationTestYmlAlignsOAuthProvidersAndCallbackUris() throws IOException {
        String content = read("src/main/resources/application-test.yml");

        assertThat(content).contains("redirect-uri: http://localhost:5173/oauth/google/callback");
        assertThat(content).contains("redirect-uri: http://localhost:5173/oauth/naver/callback");
        assertThat(content).doesNotContain("kakao:");
        assertThat(content).doesNotContain("oauth/kakao");
    }

    @Test
    void applicationYmlDefinesRetryQueueMaintenancePolicy() throws IOException {
        String content = read("src/main/resources/application.yml");

        assertThat(content).contains("retry-queue:");
        assertThat(content).contains("maintenance:");
        assertThat(content).contains("retention-days:");
        assertThat(content).contains("pending-warn-threshold:");
        assertThat(content).contains("delete-batch-size:");
    }

    @Test
    void applicationYmlDefinesEmailVerificationCleanupPolicy() throws IOException {
        String content = read("src/main/resources/application.yml");

        assertThat(content).contains("email-verification:");
        assertThat(content).contains("cleanup:");
        assertThat(content).contains("retention-hours:");
        assertThat(content).contains("batch-size:");
    }

    @Test
    void applicationTestYmlDisablesEmailVerificationCleanupScheduler() throws IOException {
        String content = read("src/main/resources/application-test.yml");

        assertThat(content).contains("email-verification:");
        assertThat(content).contains("cleanup:");
        assertThat(content).contains("enabled: false");
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
        assertThat(content).contains("push_token_id");
        assertThat(content).contains("open_key");
        assertThat(content).contains("unique key uk_notification_push_retry_jobs_open_key (open_key)");
        assertThat(content).contains("created_at");
        assertThat(content).contains("updated_at");
    }

    @Test
    void workspaceMigrationSqlDefinesRequiredTablesAndAuditColumns() throws IOException {
        String content = read("src/main/resources/db/migration/V20260303_05__create_workspaces.sql");

        assertThat(content).contains("create table if not exists workspaces");
        assertThat(content).contains("create table if not exists workspace_members");
        assertThat(content).contains("created_at");
        assertThat(content).contains("updated_at");
    }

    @Test
    void projectWorkspaceMigrationSqlAddsWorkspaceReferenceToProjects() throws IOException {
        String content = read("src/main/resources/db/migration/V20260303_06__add_workspace_id_to_projects.sql");

        assertThat(content).contains("alter table projects");
        assertThat(content).contains("add column workspace_id");
        assertThat(content).contains("idx_projects_workspace_id");
    }

    @Test
    void projectWorkspaceConstraintMigrationSqlEnforcesNotNullAndForeignKey() throws IOException {
        String content = read("src/main/resources/db/migration/V20260303_07__enforce_projects_workspace_fk.sql");

        assertThat(content).contains("update projects");
        assertThat(content).contains("modify column workspace_id bigint not null");
        assertThat(content).contains("foreign key (workspace_id) references workspaces(id)");
    }

    @Test
    void workspaceSoftDeleteMigrationSqlAddsDeletedAtColumn() throws IOException {
        String content = read("src/main/resources/db/migration/V20260303_08__add_deleted_at_to_workspaces.sql");

        assertThat(content).contains("alter table workspaces");
        assertThat(content).contains("add column deleted_at datetime(6) null");
        assertThat(content).contains("idx_workspaces_deleted_at");
    }

    @Test
    void retryCleanupIndexMigrationSqlAddsMaintenanceDeleteIndexes() throws IOException {
        String content = read("src/main/resources/db/migration/V20260304_01__add_retry_cleanup_indexes.sql");

        assertThat(content).contains("idx_attachment_cleanup_jobs_completed_updated_id");
        assertThat(content).contains("idx_invitation_email_retry_jobs_completed_updated_id");
        assertThat(content).contains("idx_notification_push_retry_jobs_completed_updated_id");
    }

    @Test
    void applicationProdYmlDefinesActuatorExposurePolicy() throws IOException {
        String content = read("src/main/resources/application-prod.yml");

        assertThat(content).contains("management:");
        assertThat(content).contains("mail:");
        assertThat(content).contains("enabled: ${MANAGEMENT_HEALTH_MAIL_ENABLED:false}");
        assertThat(content).contains("include: health,info");
        assertThat(content).contains("show-details: never");
    }

    @Test
    void applicationProdYmlRequiresCorsOriginsFromEnvironment() throws IOException {
        String content = read("src/main/resources/application-prod.yml");

        assertThat(content).contains("allowed-origins: ${APP_CORS_ALLOWED_ORIGINS}");
    }

    @Test
    void applicationProdYmlDefinesSecureRefreshCookiePolicy() throws IOException {
        String content = read("src/main/resources/application-prod.yml");

        assertThat(content).contains("refresh-token-name: refresh_token");
        assertThat(content).contains("refresh-token-path: /api/v1/auth");
        assertThat(content).contains("secure: true");
        assertThat(content).contains("same-site: Lax");
    }

    @Test
    void applicationYmlDefinesOauthCredentialEnvironmentKeys() throws IOException {
        String content = read("src/main/resources/application.yml");

        assertThat(content).contains("client-id: ${OAUTH_GOOGLE_CLIENT_ID");
        assertThat(content).contains("client-secret: ${OAUTH_GOOGLE_CLIENT_SECRET");
        assertThat(content).contains("redirect-uri: ${OAUTH_GOOGLE_REDIRECT_URI");
        assertThat(content).doesNotContain("OAUTH_KAKAO_CLIENT_ID");
        assertThat(content).doesNotContain("OAUTH_KAKAO_CLIENT_SECRET");
        assertThat(content).doesNotContain("OAUTH_KAKAO_REDIRECT_URI");
        assertThat(content).contains("client-id: ${OAUTH_NAVER_CLIENT_ID");
        assertThat(content).contains("client-secret: ${OAUTH_NAVER_CLIENT_SECRET");
        assertThat(content).contains("redirect-uri: ${OAUTH_NAVER_REDIRECT_URI");
    }

    @Test
    void applicationYmlImportsLocalDotEnvFilesForDeveloperSecrets() throws IOException {
        String content = read("src/main/resources/application.yml");

        assertThat(content).contains("config:");
        assertThat(content).contains("import:");
        assertThat(content).contains("optional:file:.env[.properties]");
        assertThat(content).contains("optional:file:../.env[.properties]");
    }

    @Test
    void applicationYmlDefinesEmailVerificationEnvironmentKeys() throws IOException {
        String content = read("src/main/resources/application.yml");

        assertThat(content).contains("enabled: ${APP_EMAIL_VERIFICATION_ENABLED:false}");
        assertThat(content).contains("from: ${APP_EMAIL_VERIFICATION_FROM:noreply@taskflow.local}");
        assertThat(content).contains("subject-prefix: ${APP_EMAIL_VERIFICATION_SUBJECT_PREFIX:[TaskFlow]}");
        assertThat(content).contains("verify-base-url: ${APP_EMAIL_VERIFICATION_VERIFY_BASE_URL:http://localhost:5173/verify-email}");
    }

    @Test
    void applicationYmlDefinesMailInfrastructureEnvironmentKeys() throws IOException {
        String content = read("src/main/resources/application.yml");

        assertThat(content).contains("host: ${MAIL_HOST:}");
        assertThat(content).contains("port: ${MAIL_PORT:587}");
        assertThat(content).contains("username: ${MAIL_USERNAME:}");
        assertThat(content).contains("password: ${MAIL_PASSWORD:}");
        assertThat(content).contains("auth: ${MAIL_SMTP_AUTH:true}");
        assertThat(content).contains("enable: ${MAIL_SMTP_STARTTLS_ENABLE:true}");
        assertThat(content).contains("enabled: ${MANAGEMENT_HEALTH_MAIL_ENABLED:false}");
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }
}
