package org.forif_backend.application.notification.dto;

import java.util.List;
import java.util.Map;

public record SendAlimTalkCommand(
        List<String> receivers,
        String templateCode,
        Map<String, String> variables
) {
}
