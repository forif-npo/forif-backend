package org.forif_backend.common.dto.response;

/**
 * 필드 유효성 검사 실패 등 구체적인 에러 정보를 담는 Record
 * @param field 에러가 발생한 필드명
 * @param message 에러 메시지
 * @param rejectedValue 거부된 값
 */
public record ApiErrorData(String field, String message, Object rejectedValue) {
}