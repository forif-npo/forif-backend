package org.forif_backend.web.staffrecruitment.dto;

import org.forif_backend.application.staffrecruitment.dto.StaffInterviewPlanDetail;

import java.time.LocalDate;
import java.util.List;

/**
 * 면접 일정 응답.
 *
 * 슬롯 목록을 내려주지 않는다. 날짜 × 시간 범위를 slotMinutes로 나누어 화면이 만든다.
 * 9~22에 30분이면 하루 26칸이다.
 */
public record StaffInterviewPlanResponse(
        Integer interviewPlanId,
        Integer recruitmentId,
        List<LocalDate> dates,
        int startHour,
        int endHour,
        int slotMinutes,
        boolean required,
        String guide,
        Long version
) {

    public static StaffInterviewPlanResponse from(StaffInterviewPlanDetail detail) {
        return new StaffInterviewPlanResponse(
                detail.interviewPlanId(),
                detail.recruitmentId(),
                detail.dates(),
                detail.startHour(),
                detail.endHour(),
                detail.slotMinutes(),
                detail.required(),
                detail.guide(),
                detail.version());
    }
}
