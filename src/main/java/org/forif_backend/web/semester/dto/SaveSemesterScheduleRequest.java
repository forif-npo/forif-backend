package org.forif_backend.web.semester.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.forif_backend.domain.semester.SemesterPhase;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = """
        한 학기의 모집 단계 기간을 통째로 저장합니다. 부분 수정이 아니라 전체 교체이므로,
        목록에서 빠진 단계는 삭제되어 상시 개방으로 되돌아갑니다.

        단계는 순차적이어야 하며 겹칠 수 없습니다.
        종료 시각은 포함하지 않으므로(반열림), "3월 8일까지"는 3월 9일 00:00으로 넣습니다.
        """)
public record SaveSemesterScheduleRequest(
        @NotNull List<@Valid PhaseWindowRequest> phases
) {
    public record PhaseWindowRequest(
            @Schema(description = "단계", example = "MENTOR_RECRUIT")
            @NotNull SemesterPhase phase,

            @Schema(description = "시작 시각", example = "2026-03-02T00:00:00")
            @NotNull LocalDateTime startsAt,

            @Schema(description = "종료 시각 (이 시각은 포함하지 않음)", example = "2026-03-09T00:00:00")
            @NotNull LocalDateTime endsAt
    ) {
    }
}
