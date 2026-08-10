package org.forif_backend.web.user.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record UserStudiesResponse(
        List<SemesterStudies> semesters
) {
    @Builder
    public record SemesterStudies(
            Integer year,
            Integer semester,
            String semesterLabel,
            Boolean isCurrent,
            StudyDetail study
    ) {
    }

    @Builder
    public record StudyDetail(
            Integer studyId,
            String studyName,
            String primaryMentorName,
            String secondaryMentorName,
            List<String> tags,
            String oneLiner,
            String startTime,
            String endTime,
            Integer weekDay,
            String location,
            Integer difficulty,
            String imgUrl,
            String thumbnailImage,
            boolean certificateIssued
    ) {
    }
}
