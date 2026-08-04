package org.forif_backend.application.notification.dto;

public record SendAlimTalkMessageResult(
        String receiver,
        boolean success,
        String errorCode,
        String errorMessage
) {
}
