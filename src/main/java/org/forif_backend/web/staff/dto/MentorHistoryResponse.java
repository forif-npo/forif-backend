package org.forif_backend.web.staff.dto;

import org.forif_backend.application.staff.dto.MentorHistory;

public record MentorHistoryResponse(
        Integer studyId,
        int actYear,
        int actSemester,
        String studyName
) {
    public static MentorHistoryResponse from(MentorHistory mentorHistory) {
        return new MentorHistoryResponse(
                mentorHistory.studyId(),
                mentorHistory.actYear(),
                mentorHistory.actSemester(),
                mentorHistory.studyName()
        );
    }
}
