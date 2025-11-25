package org.forif_backend.web.user.dto;

import org.forif_backend.application.user.dto.StudyApplicationDto;

import java.util.List;

/**
 * 멘티 신청서 목록 응답 DTO
 */
public record StudyApplicationsResponse(
        List<StudyApplicationDto> applications
) {
}