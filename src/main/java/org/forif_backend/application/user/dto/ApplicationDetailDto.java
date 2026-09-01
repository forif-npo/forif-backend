package org.forif_backend.application.user.dto;

/**
 * 1지망/2지망 신청 상세 정보 DTO
 */
public record ApplicationDetailDto(
        String priority,  // "PRIMARY" or "SECONDARY"
        StudyInfoDto study,
        Integer status,   // 합격 상태 (0=대기, 1=합격, 2=불합격)
        String intro      // 자기소개
) {
}