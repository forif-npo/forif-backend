package org.forif_backend.application.study.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record UserStudiesResult(
        List<SemesterStudiesInfo> semesters
) {
}