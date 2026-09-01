package org.forif_backend.application.user.dto;

import java.util.List;

/**
 * 신청한 스터디 정보 DTO
 */
public record StudyInfoDto(
        Integer studyId,
        String studyName,
        String primaryMentorName,
        String secondaryMentorName,
        List<String> tags,
        String oneLiner,
        Integer weekDay,
        String startTime,
        String endTime,
        String location,
        Integer difficulty,
        String imgUrl,
        String thumbnailImage,
        boolean autonomousStudy
) {
}
