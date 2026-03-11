package com.taskflow.backend.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "잘못된 이메일 또는 비밀번호입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "계정이 잠겨있습니다."),
    ACCOUNT_DELETED(HttpStatus.UNAUTHORIZED, "탈퇴한 계정입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "이메일 인증이 필요합니다."),
    EMAIL_VERIFICATION_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 이메일 인증 토큰입니다."),
    EMAIL_VERIFICATION_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "이메일 인증 토큰이 만료되었습니다."),
    EMAIL_VERIFICATION_RESEND_TOO_FREQUENT(HttpStatus.TOO_MANY_REQUESTS, "인증 메일을 다시 보내기까지 잠시 기다려주세요."),
    OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth 제공자입니다."),
    OAUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 OAuth 토큰입니다."),
    OAUTH_PROFILE_INVALID(HttpStatus.BAD_REQUEST, "OAuth 프로필 정보가 올바르지 않습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),

    // Project
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    PROJECT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "프로젝트 생성 한도를 초과했습니다."),
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 프로젝트 멤버입니다."),
    WORKSPACE_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 워크스페이스 멤버입니다."),
    MEMBER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "멤버 초대 한도를 초과했습니다."),
    NOT_PROJECT_MEMBER(HttpStatus.FORBIDDEN, "프로젝트 멤버가 아닙니다."),
    ONLY_OWNER_ALLOWED(HttpStatus.FORBIDDEN, "OWNER만 가능한 작업입니다."),
    CANNOT_REMOVE_LAST_OWNER(HttpStatus.BAD_REQUEST, "마지막 OWNER는 제거하거나 강등할 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트 멤버를 찾을 수 없습니다."),
    INVITEE_NOT_FOUND(HttpStatus.NOT_FOUND, "초대 대상 사용자를 찾을 수 없습니다."),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."),
    WORKSPACE_INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "워크스페이스 초대를 찾을 수 없습니다."),
    INVITATION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 초대입니다."),
    INSUFFICIENT_PERMISSION(HttpStatus.FORBIDDEN, "권한이 부족합니다."),

    // Task
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "태스크를 찾을 수 없습니다."),
    TASK_CONFLICT(HttpStatus.CONFLICT, "태스크 수정 충돌이 발생했습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "잘못된 상태 변경입니다."),
    LABEL_NOT_FOUND(HttpStatus.NOT_FOUND, "라벨을 찾을 수 없습니다."),
    LABEL_NAME_DUPLICATE(HttpStatus.CONFLICT, "프로젝트 내 라벨명이 중복됩니다."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "첨부파일을 찾을 수 없습니다."),
    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Notification not found."),
    // Common
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 부족합니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "요청 충돌이 발생했습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력 값입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
