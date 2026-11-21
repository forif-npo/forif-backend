package org.forif_backend.web.staffrecruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staffrecruitment.StaffInterviewPlanService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffInterviewPlanResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 면접 일정 조회. 지원 화면이 시간표를 그리는 데 쓴다.
 */
@Tag(name = "운영진 모집 면접 일정 (공개)", description = "지원 화면의 면접 가능 시간 시간표")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff-recruitments/{recruitmentId}/interview-plan")
public class StaffInterviewPlanController {

    private final StaffInterviewPlanService staffInterviewPlanService;

    @Operation(summary = "면접 일정 조회",
            description = "회장단이 설정한 후보 날짜와 시간 범위입니다. 설정이 없으면 data가 null입니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<StaffInterviewPlanResponse>> get(
            @Parameter(description = "모집 회차 ID") @PathVariable Integer recruitmentId
    ) {
        StaffInterviewPlanResponse response = staffInterviewPlanService.findByRecruitmentId(recruitmentId)
                .map(StaffInterviewPlanResponse::from)
                .orElse(null);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(ApiResponse.success(response));
    }
}
