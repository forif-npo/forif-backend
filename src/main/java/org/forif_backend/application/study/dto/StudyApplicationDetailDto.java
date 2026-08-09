package org.forif_backend.application.study.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.StudyStatus;

@Getter
@RequiredArgsConstructor
@Builder
public class StudyApplicationDetailDto {
    private final StudyDetailDto study;
    private final StudyStatus studyStatus;
    private final String rejectReason;
    private final boolean canModify;
}
