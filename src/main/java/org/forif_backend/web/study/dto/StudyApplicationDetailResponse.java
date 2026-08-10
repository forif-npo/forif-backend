package org.forif_backend.web.study.dto;

import org.forif_backend.application.study.dto.StudyDetailDto;
import org.forif_backend.domain.study.StudyStatus;

public record StudyApplicationDetailResponse(
        StudyDetailResponse study,
        String studyStatus,
        String rejectReason,
        boolean canModify
) {
    public static StudyApplicationDetailResponse from(
            StudyDetailDto study,
            StudyStatus studyStatus,
            String rejectReason,
            boolean canModify
    ) {
        return new StudyApplicationDetailResponse(
                StudyDetailResponse.from(study),
                studyStatus.getValue(),
                rejectReason,
                canModify
        );
    }
}
