package org.forif_backend.application.notification.dto;

public record TemplateInfo(
        String templateId,
        String name,
        String content,
        String status,
        String messageType,
        String dateCreated,
        String dateUpdated
) {
}