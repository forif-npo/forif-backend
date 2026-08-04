package org.forif_backend.application.dues.dto;

public record DuesMember(
        Long userId,
        String userName,
        String department,
        String currentStudyName,
        boolean duesPaid,
        boolean googleFormSubmitted
) {
}
