package org.forif_backend.application.user.dto;

import java.util.List;

/**
 * 멘티 신청서 목록 조회 결과 DTO
 */
public record GetStudyApplicationsResult(
        List<StudyApplicationDto> applications
) {
}