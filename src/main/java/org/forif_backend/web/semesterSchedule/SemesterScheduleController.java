package org.forif_backend.web.semesterSchedule;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.semesterSchedule.SemesterScheduleService;
import org.forif_backend.application.semesterSchedule.dto.SemesterScheduleDto;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.semesterSchedule.dto.CreateSemesterScheduleRequest;
import org.forif_backend.web.semesterSchedule.dto.SemesterScheduleResponse;
import org.forif_backend.web.semesterSchedule.dto.UpdateSemesterScheduleRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "학기 일정", description = "학기별 모집 및 행사 일정 API")
@RestController
@RequiredArgsConstructor
public class SemesterScheduleController {

    private final SemesterScheduleService semesterScheduleService;

    @Operation(summary = "전체 학기 일정 조회")
    @GetMapping("/api/v1/semester-schedules")
    public ResponseEntity<ApiResponse<List<SemesterScheduleResponse>>> getAllSchedules() {
        List<SemesterScheduleResponse> response = semesterScheduleService.getAllSchedules().stream()
                .map(SemesterScheduleResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "특정 학기 일정 조회")
    @GetMapping("/api/v1/semester-schedules/{year}/{semester}")
    public ResponseEntity<ApiResponse<List<SemesterScheduleResponse>>> getSchedulesByYearAndSemester(
            @Parameter(description = "연도 (예: 2025)") @PathVariable int year,
            @Parameter(description = "학기 (1 또는 2)") @PathVariable int semester
    ) {
        List<SemesterScheduleResponse> response = semesterScheduleService.getSchedulesByYearAndSemester(year, semester).stream()
                .map(SemesterScheduleResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "학기 일정 생성 (어드민 전용)")
    @PostMapping("/api/v1/admin/semester-schedules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SemesterScheduleResponse>> createSchedule(
            @RequestBody @Valid CreateSemesterScheduleRequest request
    ) {
        SemesterScheduleDto dto = semesterScheduleService.createSchedule(
                request.getActYear(),
                request.getActSemester(),
                request.getScheduleType(),
                request.getScheduledAt()
        );
        return ResponseEntity.ok(ApiResponse.success(SemesterScheduleResponse.from(dto)));
    }

    @Operation(summary = "학기 일정 수정 (어드민 전용)")
    @PatchMapping("/api/v1/admin/semester-schedules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SemesterScheduleResponse>> updateSchedule(
            @Parameter(description = "일정 ID") @PathVariable Long id,
            @RequestBody @Valid UpdateSemesterScheduleRequest request
    ) {
        SemesterScheduleDto dto = semesterScheduleService.updateSchedule(
                id,
                request.getScheduleType(),
                request.getScheduledAt()
        );
        return ResponseEntity.ok(ApiResponse.success(SemesterScheduleResponse.from(dto)));
    }

    @Operation(summary = "학기 일정 삭제 (어드민 전용)")
    @DeleteMapping("/api/v1/admin/semester-schedules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @Parameter(description = "일정 ID") @PathVariable Long id
    ) {
        semesterScheduleService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
