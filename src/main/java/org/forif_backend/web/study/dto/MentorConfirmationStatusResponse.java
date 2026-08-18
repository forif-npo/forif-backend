package org.forif_backend.web.study.dto;

import org.forif_backend.application.study.dto.MentorConfirmationStatusResult;

public record MentorConfirmationStatusResponse(
        boolean issued,
        String confirmationUrl
) {
    public static MentorConfirmationStatusResponse from(MentorConfirmationStatusResult result) {
        return new MentorConfirmationStatusResponse(result.issued(), result.confirmationUrl());
    }
}
