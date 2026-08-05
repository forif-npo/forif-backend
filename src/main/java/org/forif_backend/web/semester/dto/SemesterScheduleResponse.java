package org.forif_backend.web.semester.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.forif_backend.application.semester.dto.SemesterScheduleInfo;

import java.time.LocalDateTime;

public record SemesterScheduleResponse(
        Long id,

        @Schema(description = "연도", example = "2026")
        int actYear,

        @Schema(description = "학기", example = "2")
        int actSemester,

        @Schema(description = "단계", example = "MENTOR_RECRUIT")
        String phase,

        @Schema(description = "단계 표기", example = "멘토 모집")
        String phaseLabel,

        @Schema(description = "시작 시각 (이 시각부터 포함)")
        LocalDateTime startsAt,

        @Schema(description = "종료 시각 (이 시각은 포함하지 않음)")
        LocalDateTime endsAt,

        @Schema(description = "지금 열려 있는지")
        boolean open
) {
    public static SemesterScheduleResponse from(SemesterScheduleInfo info) {
        return new SemesterScheduleResponse(
                info.id(),
                info.actYear(),
                info.actSemester(),
                info.phase().name(),
                info.phaseLabel(),
                info.startsAt(),
                info.endsAt(),
                info.open()
        );
    }
}
