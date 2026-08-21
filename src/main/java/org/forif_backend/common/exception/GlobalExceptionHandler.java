package org.forif_backend.common.exception;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
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
        log.warn("ForifException caught: code={}, message={}", errorCode.getCode(), errorCode.getMessage());

        ApiResponse<?> response;
        if (e.getErrorDataList() != null && !e.getErrorDataList().isEmpty()) {
            response = ApiResponse.error(errorCode, e.getErrorDataList());
        } else {
            response = ApiResponse.error(errorCode);
        }
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<?>> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException e) {
        log.warn("동시 수정 충돌: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.STUDY_APPLICATION_UPDATE_CONFLICT;
        return new ResponseEntity<>(ApiResponse.error(errorCode), errorCode.getHttpStatus());
    }

    /**
     * 필수 요청 파라미터 누락 처리
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.warn("Missing required parameter: {}", e.getParameterName());
        ErrorCode errorCode = ErrorCode.MISSING_PARAMETER;
        List<ApiErrorData> errors = List.of(new ApiErrorData(e.getParameterName(), errorCode.getMessage(), null));
        ApiResponse<?> response = ApiResponse.error(errorCode, errors);
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    /**
     * 요청 파라미터 타입 불일치 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch for parameter: {}", e.getName());
        ErrorCode errorCode = ErrorCode.TYPE_MISMATCH;
        List<ApiErrorData> errors = List.of(new ApiErrorData(e.getName(), errorCode.getMessage(), e.getValue()));
        ApiResponse<?> response = ApiResponse.error(errorCode, errors);
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    /**
     * @Valid 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getBindingResult());
        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        List<ApiErrorData> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorData(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()
                ))
                .collect(Collectors.toList());
        ApiResponse<?> response = ApiResponse.error(errorCode, errors);
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    /**
     * @PreAuthorize 등 인가 실패 처리 (500이 아닌 403으로 응답)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException e) {
        log.warn("권한 부족: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.INSUFFICIENT_PERMISSION;
        return new ResponseEntity<>(ApiResponse.error(errorCode), errorCode.getHttpStatus());
    }

    /**
     * 업로드 용량 초과는 컨트롤러 진입 전에 발생하므로 별도로 400으로 응답한다.
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handleMaxUploadSizeExceeded(
            org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        log.warn("업로드 용량 초과: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_FILE_ATTACHMENT;
        return new ResponseEntity<>(ApiResponse.error(errorCode), errorCode.getHttpStatus());
    }

    /**
     * 처리하지 못한 나머지 모든 예외를 처리하는 최종 핸들러
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGlobalException(Exception e) {
        log.error("알 수 없는 예외: {}", e.getMessage(), e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(ApiResponse.error(errorCode), errorCode.getHttpStatus());
    }
}
