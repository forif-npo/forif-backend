package org.forif_backend.web.staffrecruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.staffrecruitment.StaffInterviewScheduleService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffInterviewScheduleRequests.HostSlotsRequest;
import org.forif_backend.web.staffrecruitment.dto.StaffInterviewScheduleRequests.SlotRequest;
import org.forif_backend.web.staffrecruitment.dto.StaffInterviewScheduleResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 면접 일정 조율. 회장단만 시간을 정하고 지원자를 배정한다.
 * 쓰기 작업은 모집이 마감된 회차에서만 한다.
 */
@Tag(name = "운영진 모집 면접 일정 조율 (회장단)", description = "회장단 가능 시간, 지원자별 면접 시각 배정, 일정 확정")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/staff-recruitments/{recruitmentId}/interview-schedule")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffInterviewScheduleController {

    private final StaffInterviewScheduleService staffInterviewScheduleService;
    private final StaffAccountService staffAccountService;

    @Operation(summary = "면접 일정 조율 조회",
            description = "면접 일정, 회장단 가능 시간, 지원자별 가능 시간과 배정 시각을 한 번에 돌려줍니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<StaffInterviewScheduleResponse>> get(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "모집 회차 ID") @PathVariable Integer recruitmentId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(StaffInterviewScheduleResponse.from(staffInterviewScheduleService.getSchedule(recruitmentId)));
    }

    @Operation(summary = "회장단 가능 시간 저장", description = "보낸 목록으로 맞춥니다.")
    @PutMapping("/host-slots")
    public ResponseEntity<ApiResponse<StaffInterviewScheduleResponse>> replaceHostSlots(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Valid @RequestBody HostSlotsRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(StaffInterviewScheduleResponse.from(
                staffInterviewScheduleService.replaceHostSlots(recruitmentId, request.toSlots())));
    }

    @Operation(summary = "지원자 면접 시각 배정·변경",
            description = "한 칸에 한 명만 들어갑니다. 이미 배정했으면 그 칸으로 옮깁니다.")
    @PutMapping("/assignments/{applicationId}")
    public ResponseEntity<ApiResponse<StaffInterviewScheduleResponse>> assign(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Parameter(description = "지원서 ID") @PathVariable Integer applicationId,
            @Valid @RequestBody SlotRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(StaffInterviewScheduleResponse.from(staffInterviewScheduleService.assign(
                recruitmentId, applicationId, request.date(), request.startMinute())));
    }

    @Operation(summary = "지원자 면접 시각 배정 취소")
    @DeleteMapping("/assignments/{applicationId}")
    public ResponseEntity<ApiResponse<StaffInterviewScheduleResponse>> unassign(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @PathVariable Integer applicationId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(StaffInterviewScheduleResponse.from(
                staffInterviewScheduleService.unassign(recruitmentId, applicationId)));
    }

    @Operation(summary = "면접 일정 확정",
            description = "배정하지 못한 지원자가 있어도 확정할 수 있습니다. 확정 뒤에도 시각을 옮길 수 있습니다.")
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<StaffInterviewScheduleResponse>> confirm(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(StaffInterviewScheduleResponse.from(staffInterviewScheduleService.confirm(recruitmentId)));
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(body));
    }
}
