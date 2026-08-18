package org.forif_backend.web.study.dto;

import org.forif_backend.application.study.dto.AdminStudyDto;
import org.forif_backend.application.study.dto.StudyTagDto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminStudyResponse(
        Integer id,
        String studyName,
        String primaryMentorName,
        String secondaryMentorName,
        List<String> tags,
        String oneLiner,
        long menteeCount,
        String recruitStatus,
        Integer weekDay,
        String difficulty,
        String studyStatus,
        String rejectReason,
        LocalDateTime createdAt
) {
    public static AdminStudyResponse from(AdminStudyDto dto) {
        List<String> tagNames = dto.getTags().stream()
                .map(StudyTagDto::getName)
                .toList();

        String recruitStatusValue = dto.getRecruitStatus() != null
                ? dto.getRecruitStatus().getValue()
                : null;

        String studyStatusValue = dto.getStudyStatus() != null
                ? dto.getStudyStatus().getValue()
                : null;

        String difficultyValue = dto.getDifficulty() != null
                ? dto.getDifficulty().getValue()
                : null;

        return new AdminStudyResponse(
                dto.getId(),
                dto.getStudyName(),
                dto.getPrimaryMentorName(),
                dto.getSecondaryMentorName(),
                tagNames,
                dto.getOneLiner(),
                dto.getMenteeCount(),
                recruitStatusValue,
                dto.getWeekDay(),
                difficultyValue,
                studyStatusValue,
                dto.getRejectReason(),
                dto.getCreatedAt()
        );
    }
}
