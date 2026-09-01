package org.forif_backend.application.user.dto;

import java.time.LocalDate;

/**
 * 멘티 신청서 정보 DTO
 */
public record StudyApplicationDto(
        Long userApplyId,
        Integer applyYear,
        Integer applySemester,
        LocalDate applyDate,
        String applyPath,
        Integer payStatus,  // 결제 상태 (0=미결제, 1=완료)
        ApplicationDetailDto primaryApplication,
        ApplicationDetailDto secondaryApplication  // null 가능
) {
}