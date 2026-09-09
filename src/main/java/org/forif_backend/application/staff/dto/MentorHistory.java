package org.forif_backend.application.staff.dto;

import org.forif_backend.domain.study.Study;

public record MentorHistory(
        Integer studyId,
        int actYear,
        int actSemester,
        String studyName
) {
    public static MentorHistory from(Study study) {
        return new MentorHistory(
                study.getId(),
                study.getActYear(),
                study.getActSemester(),
                study.getStudyName()
        );
    }
}
