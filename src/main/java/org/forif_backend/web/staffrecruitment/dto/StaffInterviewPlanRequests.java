package org.forif_backend.web.staffrecruitment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 면접 일정 설정 요청. 후보 날짜는 보낸 목록으로 맞춘다.
 */
public final class StaffInterviewPlanRequests {

    private StaffInterviewPlanRequests() {
    }

    public record SaveRequest(
            @NotEmpty List<@NotNull LocalDate> dates,
            @Min(0) @Max(23) int startHour,
            @Min(1) @Max(24) int endHour,
            /** 한 칸의 길이(분). 10·15·20·30·60만 받는다. 생략하면 30분이다. */
            Integer slotMinutes,
            Boolean required,
            @Size(max = 200) String guide,
            /** 최초 설정에서는 생략하고, 기존 설정을 수정할 때는 조회한 값을 보낸다. */
            @Min(0) Long version
    ) {
        public boolean requiredOrDefault() {
            return required == null || required;
        }

        public int slotMinutesOrDefault() {
            return slotMinutes == null ? 30 : slotMinutes;
        }
    }
}
