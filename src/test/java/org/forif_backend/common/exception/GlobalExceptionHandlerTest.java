package org.forif_backend.common.exception;

import org.forif_backend.common.dto.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 데이터_무결성_제약_위반은_409로_응답한다() {
        ResponseEntity<ApiResponse<?>> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("foreign key constraint fails"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode())
                .isEqualTo(ErrorCode.DATA_INTEGRITY_VIOLATION.getCode());
        assertThat(response.getBody().message())
                .isEqualTo(ErrorCode.DATA_INTEGRITY_VIOLATION.getMessage());
    }
}
