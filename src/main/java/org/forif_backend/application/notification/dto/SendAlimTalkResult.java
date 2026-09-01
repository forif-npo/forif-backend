package org.forif_backend.application.notification.dto;

import java.util.List;

public record SendAlimTalkResult(
        String templateId,
        List<SendAlimTalkMessageResult> results
) {
}
