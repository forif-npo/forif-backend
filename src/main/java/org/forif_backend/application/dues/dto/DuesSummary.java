package org.forif_backend.application.dues.dto;

public record DuesSummary(
        int totalCount,
        int duesPaidCount,
        int googleFormSubmittedCount,
        int completedCount
) {
}
