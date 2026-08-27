package org.forif_backend.application.dues.dto;

public record DuesMember(
        Long userId,
        String userName,
        String department,
        boolean duesPaid,
        boolean googleFormSubmitted
) {
}
