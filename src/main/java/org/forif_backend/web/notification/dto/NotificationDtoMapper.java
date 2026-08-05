package org.forif_backend.web.notification.dto;

import org.forif_backend.application.notification.dto.SendAlimTalkCommand;
import org.forif_backend.application.notification.dto.SendAlimTalkMessageResult;
import org.forif_backend.application.notification.dto.SendAlimTalkResult;

import java.util.List;

public class NotificationDtoMapper {

    public static SendAlimTalkCommand toCommand(SendAlimTalkRequest request) {
        return new SendAlimTalkCommand(
                request.receivers(),
                request.templateCode(),
                request.variables()
        );
    }

    public static SendAlimTalkResponse toResponse(SendAlimTalkResult result) {
        List<SendAlimTalkMessageResult> results = result.results();

        long successCount = results.stream()
                .filter(SendAlimTalkMessageResult::success)
                .count();

        long failureCount = results.stream()
                .filter(r -> !r.success())
                .count();

        return new SendAlimTalkResponse(
                result.templateId(),
                results.size(),
                (int) successCount,
                (int) failureCount,
                results.stream()
                        .map(r -> new SendAlimTalkMessageResponse(
                                r.receiver(),
                                r.success(),
                                r.errorCode(),
                                r.errorMessage()
                        ))
                        .toList()
        );
    }
}
