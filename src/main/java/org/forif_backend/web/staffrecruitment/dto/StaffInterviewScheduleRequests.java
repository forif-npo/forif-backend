package org.forif_backend.web.staffrecruitment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewScheduleDetail;

import java.time.LocalDate;
import java.util.List;

/**
 * 면접 일정 조율 요청. 칸은 날짜와 시작 분(자정 기준)으로 가리킨다. 09:30이면 570이다.
 */
public final class StaffInterviewScheduleRequests {

    private StaffInterviewScheduleRequests() {
    }

    public record SlotRequest(
            @NotNull LocalDate date,
            @Min(0) @Max(1439) int startMinute
    ) {
        StaffInterviewScheduleDetail.Slot toSlot() {
            return new StaffInterviewScheduleDetail.Slot(date, startMinute);
        }
    }

    /** 회장단 가능 시간 전체. 보낸 목록으로 맞춘다. */
    public record HostSlotsRequest(
            @NotNull List<@NotNull @Valid SlotRequest> slots
    ) {
        public List<StaffInterviewScheduleDetail.Slot> toSlots() {
            return slots.stream().map(SlotRequest::toSlot).toList();
        }
    }
}
