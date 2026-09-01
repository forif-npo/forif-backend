package org.forif_backend.web.semester.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.forif_backend.domain.semester.SemesterPhase;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = """
        한 학기의 모집 단계와 스터디 시작 시각을 통째로 저장합니다. 부분 수정이 아니라 전체 교체이므로,
        목록에서 빠진 모집 단계는 삭제되어 상시 개방으로 되돌아갑니다.
        스터디 시작 시각이 없으면 승인된 스터디는 자동으로 개설 상태로 전환되지 않습니다.

        단계는 순차적이어야 하며 겹칠 수 없습니다.
        모든 시각은 분(HH:mm) 단위까지만 입력합니다.
        종료 시각은 포함하지 않는 반열림 구간입니다.
        """)
public record SaveSemesterScheduleRequest(
        @NotNull List<@Valid PhaseWindowRequest> phases
) {
    public record PhaseWindowRequest(
            @Schema(description = "단계", example = "MENTOR_RECRUIT")
            @NotNull SemesterPhase phase,

            @Schema(description = "시작 시각 (분 단위까지 입력)", example = "2026-03-02T09:30:00")
            @NotNull LocalDateTime startsAt,

            @Schema(description = "종료 시각 (이 시각은 포함하지 않음, 분 단위까지 입력)", example = "2026-03-09T18:30:00")
            @NotNull LocalDateTime endsAt
    ) {
    }
}
