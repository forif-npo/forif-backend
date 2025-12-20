package org.forif_backend.application.user.dto;

import java.util.List;

/**
 * 멘토 신청서 목록 조회 결과 DTO
 */
public record GetStudyCreationApplicationsResult(
        List<StudyCreationApplicationDto> applications
) {
}