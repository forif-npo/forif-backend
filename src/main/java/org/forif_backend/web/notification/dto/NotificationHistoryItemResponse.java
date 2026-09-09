package org.forif_backend.web.notification.dto;

import org.forif_backend.application.notification.dto.NotificationHistoryItem;

public record NotificationHistoryItemResponse(
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
    public static NotificationHistoryItemResponse from(NotificationHistoryItem item) {
        return new NotificationHistoryItemResponse(
                item.messageId(),
                item.templateId(),
                item.receiver(),
                item.status(),
                item.statusCode(),
                item.createdAt(),
                item.processedAt(),
                item.reportedAt(),
                item.updatedAt()
        );
    }
}
