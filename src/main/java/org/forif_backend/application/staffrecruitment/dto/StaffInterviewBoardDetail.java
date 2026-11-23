package org.forif_backend.application.staffrecruitment.dto;

import org.forif_backend.application.staffrecruitment.dto.StaffInterviewScheduleDetail.Slot;
import org.forif_backend.domain.staffrecruitment.StaffInterviewStatus;

import java.util.List;

/**
 * 면접 관리 보드. 면접 전·면접 완료·합격·불합격 네 칸에 지원자 카드를 놓는다.
 * 면접 길이는 slotMinutes이고, 면접 일정이 없는 회차면 null이다.
 */
public record StaffInterviewBoardDetail(
        Integer slotMinutes,
        List<Card> cards
) {

    public record Card(
            Integer applicationId,
            String applicantName,
            Slot assignedSlot,
            String firstDepartment,
            String secondDepartment,
            StaffInterviewStatus status
    ) {
    }
}
