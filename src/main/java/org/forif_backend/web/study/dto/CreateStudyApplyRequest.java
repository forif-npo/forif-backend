package org.forif_backend.web.study.dto;

import java.util.List;

public record CreateStudyApplyRequest(
        String title,
        String subTitle,
        Long studyTagId,
        String goal,
        String explanation,
        Boolean isOnline,
        Long studyLocationId,
        String studyLocationDetail,
        Integer weekDay,
        String startTime,
        String endTime,
        List<StudyPlan> studyPlanList,
        Integer difficulty,
        String SelectionCriteria,
        Integer capacity,
        Boolean requiresInterview,
        String InterviewDate,
        List<Reference> references
) {
    public record StudyPlan(
            String title,
            String content
    ) {}

    public record Reference(
            String week,
            String date,
            String topic,
            List<String> content
    ) {}
}
