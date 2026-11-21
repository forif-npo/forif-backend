package org.forif_backend.application.staffrecruitment.dto;

import org.forif_backend.domain.staffrecruitment.StaffInterviewPlan;

import java.time.LocalDate;
import java.util.List;

/**
 * 모집 회차의 면접 일정. 공개 지원 화면과 회장단 관리 화면이 같은 모델을 쓴다.
 * 슬롯은 저장하지 않고 후보 날짜와 시간 범위로 화면이 만든다.
 */
public record StaffInterviewPlanDetail(
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

    public static StaffInterviewPlanDetail from(StaffInterviewPlan plan) {
        return new StaffInterviewPlanDetail(
                plan.getId(),
                plan.getRecruitment().getId(),
                plan.candidateDates(),
                plan.getStartHour(),
                plan.getEndHour(),
                plan.getSlotMinutes(),
                plan.isRequired(),
                plan.getGuide(),
                plan.getVersion());
    }
}
