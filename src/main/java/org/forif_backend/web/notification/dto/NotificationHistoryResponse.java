package org.forif_backend.web.notification.dto;

import org.forif_backend.application.notification.dto.NotificationHistoryPage;

import java.util.List;

public record NotificationHistoryResponse(
        List<NotificationHistoryItemResponse> content,
        String nextCursor,
        boolean hasNext
) {
    public static NotificationHistoryResponse from(NotificationHistoryPage page) {
        return new NotificationHistoryResponse(
                page.content().stream().map(NotificationHistoryItemResponse::from).toList(),
                page.nextCursor(),
                page.hasNext()
        );
    }
}
