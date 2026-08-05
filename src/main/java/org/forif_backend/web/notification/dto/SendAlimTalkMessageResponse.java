package org.forif_backend.web.notification.dto;

public record SendAlimTalkMessageResponse(
        String receiver,
        boolean success,
        String errorCode,
        String errorMessage
) {
}
