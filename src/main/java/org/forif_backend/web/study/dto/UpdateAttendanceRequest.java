package org.forif_backend.web.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "출석 기록 일괄 저장 요청")
public record UpdateAttendanceRequest(
        @Schema(description = "저장할 출석 기록 목록")
        @NotEmpty
        @Valid
        List<AttendanceItem> attendances
) {
    @Schema(description = "출석 기록 항목")
    public record AttendanceItem(
            @Schema(description = "멘티 유저 ID(학번)", example = "2024097956")
            @NotNull
            Long userId,

            @Schema(description = "주차 (1부터 시작)", example = "1")
            @Min(1)
            int weekNum,

            @Schema(description = "출석 상태 (present / absent)", example = "present")
            @NotNull
            String status,

            @Schema(description = "스터디 진행일 (yyyy-MM-dd, 선택)", example = "2026-03-06")
            String studyDate
    ) {
    }
}
