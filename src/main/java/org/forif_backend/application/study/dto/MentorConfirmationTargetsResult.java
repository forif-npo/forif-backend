package org.forif_backend.application.study.dto;

import java.util.List;

public record MentorConfirmationTargetsResult(
        Integer studyId,
        String studyName,
        int actYear,
        int actSemester,
        List<Target> targets
) {
    public record Target(
            Long userId,
            String userName,
            String department,
            int confirmationStatus
    ) {
    }
}
