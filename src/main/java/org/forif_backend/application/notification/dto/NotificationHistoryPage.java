package org.forif_backend.application.notification.dto;

import java.util.List;

/**
 * Solapi의 startKey/nextKey 기반 페이지를 API에 노출하기 위한 응답 모델이다.
 */
public record NotificationHistoryPage(
        List<NotificationHistoryItem> content,
        String nextCursor,
        boolean hasNext
) {
}
