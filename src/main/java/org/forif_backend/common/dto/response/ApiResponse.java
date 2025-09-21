package org.forif_backend.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 모든 API 응답을 위한 표준 형식 Record
 * @param timestamp 응답 시간
 * @param data 성공 시 전달될 데이터 또는 실패 시 전달될 상세 에러 데이터
 * @param errorCode 실패 시 전달될 에러 코드
 * @param message 성공 또는 실패 메시지
 * @param <T> 데이터의 타입
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        long timestamp,
        T data,
        String errorCode,
        String message
) {

    // 성공 응답 (데이터 포함)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(System.currentTimeMillis(), data, null, "Success");
    }

    // 성공 응답 (데이터 없이 메시지만 포함)
    public static <T> ApiResponse<T> successWithMsg(String message) {
        return new ApiResponse<>(System.currentTimeMillis(), null, null, message);
    }

    // 실패 응답 (메시지만 포함)
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(System.currentTimeMillis(), null, errorCode, message);
    }

    // 실패 응답 (상세 데이터 포함, 예: 유효성 검사 실패 목록)
    public static <T> ApiResponse<T> error(String errorCode, String message, T data) {
        return new ApiResponse<>(System.currentTimeMillis(), data, errorCode, message);
    }
}