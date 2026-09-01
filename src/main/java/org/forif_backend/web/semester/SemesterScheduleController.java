package org.forif_backend.web.semester;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.semester.SemesterScheduleService;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.semester.dto.SemesterScheduleInfo;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.semester.dto.SaveSemesterScheduleRequest;
import org.forif_backend.web.semester.dto.SemesterScheduleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "학기 일정", description = "학기별 모집 단계와 스터디 시작 시각 API")
@RestController
@RequiredArgsConstructor
public class SemesterScheduleController {

    private final SemesterScheduleService semesterScheduleService;
    private final SemesterService semesterService;
    private final StaffAccountService staffAccountService;

    @Operation(summary = "현재 학기 일정 조회",
            description = "활동 학기의 모집 단계와 스터디 시작 시각을 조회합니다. 설정되지 않은 멘티 모집·수락/거절 단계는 닫힌 상태이며, 그 외 모집 단계는 상시 개방입니다.")
    @GetMapping("/api/v1/semester-schedules/current")
    public ResponseEntity<ApiResponse<List<SemesterScheduleResponse>>> getCurrentSchedules() {
        SemesterInfo active = semesterService.getActive();
        return ResponseEntity.ok(ApiResponse.success(toResponse(
                semesterScheduleService.getSchedules(active.actYear(), active.actSemester()))));
    }

    @Operation(summary = "특정 학기 일정 조회")
    @GetMapping("/api/v1/semester-schedules/{year}/{semester}")
    public ResponseEntity<ApiResponse<List<SemesterScheduleResponse>>> getSchedules(
            @Parameter(description = "연도 (예: 2026)") @PathVariable int year,
            @Parameter(description = "학기 (1 또는 2)") @PathVariable int semester
    ) {
        return ResponseEntity.ok(ApiResponse.success(toResponse(
                semesterScheduleService.getSchedules(year, semester))));
    }

    @Operation(summary = "학기 일정 저장 (회장단 전용)",
            description = """
                    한 학기의 모집 단계와 스터디 시작 시각을 통째로 저장합니다. 부분 수정이 아니라 전체 교체이므로,
                    목록에서 빠진 멘티 모집·수락/거절 단계는 닫힌 상태가 되고, 그 외 모집 단계는 상시 개방으로 돌아갑니다.
                    스터디 시작 시각이 없으면 승인된 스터디는 자동으로 개설 상태로 전환되지 않습니다.

                    단계는 멘토 모집 → 멘토 수락/거절 → 멘티 모집 → 멘티 수락/거절 순서여야 하며,
                    앞 단계가 끝난 뒤에 다음 단계가 시작되어야 합니다. 겹치면 거부됩니다.
                    """)
    @PutMapping("/api/v1/admin/semester-schedules/{year}/{semester}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SemesterScheduleResponse>>> saveSchedules(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "연도 (예: 2026)") @PathVariable int year,
            @Parameter(description = "학기 (1 또는 2)") @PathVariable int semester,
            @Valid @RequestBody SaveSemesterScheduleRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);

        List<SemesterScheduleService.PhaseWindow> windows = request.phases().stream()
                .map(p -> new SemesterScheduleService.PhaseWindow(p.phase(), p.startsAt(), p.endsAt()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(toResponse(
                semesterScheduleService.replaceSchedules(year, semester, windows, userId))));
    }

    private List<SemesterScheduleResponse> toResponse(List<SemesterScheduleInfo> infos) {
        return infos.stream().map(SemesterScheduleResponse::from).toList();
    }
}
