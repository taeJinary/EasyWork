package com.taskflow.backend.global.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.taskflow.backend.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final String errorCode;
    private final List<ErrorDetail> errors;
    private final LocalDateTime timestamp;

    private ApiResponse(
            boolean success,
            T data,
            String message,
            String errorCode,
            List<ErrorDetail> errors
    ) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.errorCode = errorCode;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null, null);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.getMessage(), null);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode, List<ErrorDetail> errors) {
        return failure(errorCode, errorCode.getMessage(), errors);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return failure(errorCode, message, null);
    }

    public static ApiResponse<Void> failure(
            ErrorCode errorCode,
            String message,
            List<ErrorDetail> errors
    ) {
        return new ApiResponse<>(false, null, message, errorCode.name(), errors);
    }

    public record ErrorDetail(String field, Object value, String reason) {
    }
}
