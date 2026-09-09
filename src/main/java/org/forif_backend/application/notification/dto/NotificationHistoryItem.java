package org.forif_backend.application.notification.dto;

/**
 * Solapi 메시지 목록 응답에서 알림톡 이력 화면에 필요한 값만 추린 항목이다.
 * 시간 문자열은 Solapi가 반환한 값을 변환하지 않고 보존한다.
 */
public record NotificationHistoryItem(
        String messageId,
        String templateId,
        String receiver,
        String status,
        String statusCode,
        String createdAt,
        String processedAt,
        String reportedAt,
        String updatedAt
) {
}
