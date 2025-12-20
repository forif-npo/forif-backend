package org.forif_backend.application.notification.dto;

import java.util.List;

public record SendAlimTalkCommand(
        List<String> receivers,
        String templateCode,
        String studyName,
        String responseSchedule,
        String dateTime,
        String location,
        String url
) {
}