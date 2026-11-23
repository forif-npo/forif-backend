package org.forif_backend.application.staffrecruitment.dto;

import org.forif_backend.domain.staffrecruitment.StaffInterviewStatus;

import java.time.LocalDate;

/** 지원자 본인에게 공개할 면접 배정시각과 결과. */
public record StaffApplicationInterviewSummary(
        LocalDate date,
        Integer startMinute,
        Integer slotMinutes,
        StaffInterviewStatus status
) {
}
