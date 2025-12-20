package org.forif_backend.web.notification.dto;

import org.forif_backend.application.notification.dto.SendAlimTalkCommand;
import org.forif_backend.application.notification.dto.SendAlimTalkResult;

import java.util.List;

public class NotificationDtoMapper {

    public static SendAlimTalkCommand toCommand(SendAlimTalkRequest request) {
        return new SendAlimTalkCommand(
                request.receivers(),
                request.templateCode(),
                request.studyName(),
                request.responseSchedule(),
                request.dateTime(),
                request.location(),
                request.url()
        );
    }

    public static SendAlimTalkResponse toResponse(SendAlimTalkResult result) {
        List<String> results = result.results();

        long successCount = results.stream()
                .filter(r -> r.startsWith("Success"))
                .count();

        long failureCount = results.stream()
                .filter(r -> r.startsWith("Failed"))
                .count();

        return new SendAlimTalkResponse(
                results.size(),
                (int) successCount,
                (int) failureCount,
                results
        );
    }
}