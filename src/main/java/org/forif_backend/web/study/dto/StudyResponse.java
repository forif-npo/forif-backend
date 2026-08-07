package org.forif_backend.web.study.dto;

import java.util.List;

import org.forif_backend.application.study.dto.StudyDto;
import org.forif_backend.application.study.dto.StudyTagDto;

public record StudyResponse(
        Integer id,
        String studyName,
        String primaryMentorName,
        String secondaryMentorName,
        List<String> tags,
        String recruitStatus,
        String oneLiner,
        String explanation,
        String startTime,
        String endTime,
        Integer weekDay,
        String location,
        String difficulty,
        String imgUrl,
        String thumbnailImage,
        Integer actYear,
        Integer actSemester
) {
    public static StudyResponse from(StudyDto study) {
        List<String> tagNames = study.getTags().stream()
                .map(StudyTagDto::getName)
                .toList();

        String recruitStatusValue = study.getRecruitStatus() != null
                ? study.getRecruitStatus().getValue()
                : null;

        String difficultyValue = study.getDifficulty() != null
                ? study.getDifficulty().getValue()
                : null;

        return new StudyResponse(
                study.getId(),
                study.getStudyName(),
                study.getPrimaryMentorName(),
                study.getSecondaryMentorName(),
                tagNames,
                recruitStatusValue,
                study.getOneLiner(),
                study.getExplanation(),
                study.getStartTime(),
                study.getEndTime(),
                study.getWeekDay(),
                study.getLocation(),
                difficultyValue,
                study.getImgUrl(),
                study.getThumbnailImage(),
                study.getActYear(),
                study.getActSemester()
        );
    }

    public static List<StudyResponse> fromList(List<StudyDto> studies) {
        return studies.stream()
                .map(StudyResponse::from)
                .toList();
    }
}