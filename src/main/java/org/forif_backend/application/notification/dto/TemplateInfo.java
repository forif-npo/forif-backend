package org.forif_backend.application.notification.dto;

import java.util.List;

public record TemplateInfo(
        String templateId,
        String name,
        String content,
        String status,
        String messageType,
        String dateCreated,
        String dateUpdated,
        List<String> variables,
        List<String> buttonLinks
) {
}
