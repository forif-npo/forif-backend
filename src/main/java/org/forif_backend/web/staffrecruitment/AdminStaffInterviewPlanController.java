package org.forif_backend.web.staffrecruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.staffrecruitment.StaffInterviewPlanService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffInterviewPlanRequests.SaveRequest;
import org.forif_backend.web.staffrecruitment.dto.StaffInterviewPlanResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 면접 일정 설정. 회장단만 후보 날짜와 수집 시간 범위를 정한다.
 */
@Tag(name = "운영진 모집 면접 일정 관리 (회장단)", description = "면접 후보 날짜와 시간 범위 설정")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/staff-recruitments/{recruitmentId}/interview-plan")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffInterviewPlanController {

    private final StaffInterviewPlanService staffInterviewPlanService;
    private final StaffAccountService staffAccountService;

    @Operation(summary = "면접 일정 조회", description = "설정이 없으면 404입니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<StaffInterviewPlanResponse>> get(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "모집 회차 ID") @PathVariable Integer recruitmentId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return noStore(ApiResponse.success(
                StaffInterviewPlanResponse.from(staffInterviewPlanService.getByRecruitmentId(recruitmentId))));
    }

    @Operation(summary = "면접 일정 설정",
            description = "후보 날짜를 보낸 목록으로 맞춥니다. 최초 설정은 version을 생략하고, 수정할 때는 조회한 version을 보냅니다. 이미 제출된 지원서의 선택은 지워지지 않습니다.")
    @PutMapping
    public ResponseEntity<ApiResponse<StaffInterviewPlanResponse>> save(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Valid @RequestBody SaveRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);

        StaffInterviewPlanResponse response = StaffInterviewPlanResponse.from(
                staffInterviewPlanService.save(recruitmentId, request.dates(), request.startHour(),
                        request.endHour(), request.slotMinutesOrDefault(),
                        request.requiredOrDefault(), request.guide(), request.version()));

        return noStore(ApiResponse.success(response));
    }

    @Operation(summary = "면접 일정 삭제",
            description = "면접을 받지 않는 회차로 되돌립니다. 이미 제출된 선택은 남습니다.")
    @DeleteMapping
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        staffInterviewPlanService.delete(recruitmentId);
        return ResponseEntity.noContent().build();
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(ApiResponse<T> body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(body);
    }
}
