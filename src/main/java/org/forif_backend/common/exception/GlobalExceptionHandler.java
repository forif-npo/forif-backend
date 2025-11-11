package org.forif_backend.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.forif_backend.common.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ForifException.class)
    public ResponseEntity<ApiResponse<?>> handleForifException(ForifException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("ForifException caught: code={}, message={}", errorCode.getCode(), e.getMessage());

        // 예외에 상세 데이터가 포함되어 있는지 확인
        ApiResponse<?> response;

        // 실제 예외 메시지가 있으면 그것을 사용, 없으면 ErrorCode의 기본 메시지 사용
        String message = e.getMessage() != null ? e.getMessage() : errorCode.getMessage();

        if (e.getErrorDataList() != null && !e.getErrorDataList().isEmpty()) {
            // 상세 데이터가 있으면 data 필드에 담아 반환
            response = ApiResponse.error(errorCode.getCode(), message, e.getErrorDataList());

        } else {
            // 상세 데이터가 없으면 기존처럼 data 필드는 null로 반환
            response = ApiResponse.error(errorCode.getCode(), message);

        }
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    /**
     * 처리하지 못한 나머지 모든 예외를 처리하는 최종 핸들러
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGlobalException(Exception e) {
        log.error("알 수 없는 예외: {}", e.getMessage(), e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ApiResponse<?> response = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());

        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }
}

