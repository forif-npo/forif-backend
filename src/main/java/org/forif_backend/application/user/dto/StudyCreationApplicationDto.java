package org.forif_backend.application.user.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 멘토 신청서 정보 DTO
 */
public record StudyCreationApplicationDto(
        Integer applyId,
        String studyName,
        List<String> tags,
        String oneLiner,
        String explanation,
        String startTime,
        String endTime,
        Integer weekDay,
        String location,
        Integer difficulty,
        Integer acceptanceStatus,  // 승인 상태
        Integer actYear,
        Integer actSemester,
        String role,  // "PRIMARY_MENTOR" or "SECONDARY_MENTOR"
        String partnerMentorName,  // 파트너 멘토 이름 (null 가능)
        LocalDateTime createdAt
) {
}