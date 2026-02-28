package com.taskflow.backend.global.common.dto;

import com.taskflow.backend.global.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void success_응답은_성공_형식을_반환한다() {
        ApiResponse<String> response = ApiResponse.success("ok", "완료");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("ok");
        assertThat(response.getMessage()).isEqualTo("완료");
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getErrors()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void failure_응답은_에러_형식을_반환한다() {
        ApiResponse.ErrorDetail detail = new ApiResponse.ErrorDetail("email", "invalid", "올바른 이메일 형식이 아닙니다.");
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.INVALID_INPUT, List.of(detail));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo(ErrorCode.INVALID_INPUT.getMessage());
        assertThat(response.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT.name());
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getTimestamp()).isNotNull();
    }
}
