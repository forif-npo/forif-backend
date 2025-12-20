package org.forif_backend.web.user.dto;

import org.forif_backend.application.user.dto.StudyCreationApplicationDto;

import java.util.List;

/**
 * 멘토 신청서 목록 응답 DTO
 */
public record StudyCreationApplicationsResponse(
        List<StudyCreationApplicationDto> applications
) {
}