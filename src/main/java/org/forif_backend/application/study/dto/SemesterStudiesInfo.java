package org.forif_backend.application.study.dto;

import lombok.Builder;

@Builder
public record SemesterStudiesInfo(
        Integer year,
        Integer semester,
        String semesterLabel,
        Boolean isCurrent,
        StudyInfo study
) {
}