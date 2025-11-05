package org.forif_backend.common.exception;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.forif_backend.common.dto.response.ApiErrorData;
import org.forif_backend.common.dto.response.ApiResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ForifException.class)
    public ResponseEntity<ApiResponse<?>> handleForifException(ForifException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("ForifException caught: code={}, message={}", errorCode.getCode(), e.getMessage());

        // 예외에 상세 데이터가 포함되어 있는지 확인
        ApiResponse<?> response;

        if (e.getErrorDataList() != null && !e.getErrorDataList().isEmpty()) {
            // 상세 데이터가 있으면 data 필드에 담아 반환
            response = ApiResponse.error(errorCode.getCode(), errorCode.getMessage(), e.getErrorDataList());

        } else {
            // 상세 데이터가 없으면 기존처럼 data 필드는 null로 반환
            response = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());

        }
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    /**
     * 필수 요청 파라미터 누락 처리
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.warn("Missing required parameter: {}", e.getParameterName());
        String message = String.format("필수 파라미터가 누락되었습니다: %s", e.getParameterName());
        ApiResponse<?> response = ApiResponse.error("BAD_REQUEST", message);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 요청 파라미터 타입 불일치 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch for parameter: {}", e.getName());
        String message = String.format("파라미터 타입이 올바르지 않습니다: %s", e.getName());
        ApiResponse<?> response = ApiResponse.error("BAD_REQUEST", message);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * @Valid 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getBindingResult());
        List<ApiErrorData> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorData(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()
                ))
                .collect(Collectors.toList());
        
        ApiResponse<?> response = ApiResponse.error("VALIDATION_FAILED", "입력값 검증에 실패했습니다", errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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
