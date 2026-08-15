package org.forif_backend.application.study.dto;

public record MentorConfirmationStatusResult(
        boolean issued,
        String confirmationUrl
) {
}
