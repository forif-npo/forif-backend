package org.forif_backend.web.staffrecruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staffrecruitment.StaffRecruitmentService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffRecruitmentResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 운영진 모집 조회. 로그인 없이 호출하며 초안은 노출하지 않는다.
 */
@Tag(name = "운영진 모집 (공개)", description = "모집 배너·안내 화면이 사용하는 공개 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff-recruitments")
public class StaffRecruitmentController {

    private final StaffRecruitmentService staffRecruitmentService;

    @Operation(summary = "현재 모집 회차 조회",
            description = "게시 중인 회차를 반환합니다. 게시된 회차가 없으면 data가 null입니다.")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<StaffRecruitmentResponse>> getCurrent() {
        StaffRecruitmentResponse response = staffRecruitmentService.getPublicCurrent()
                .map(StaffRecruitmentResponse::from)
                .orElse(null);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "모집 회차 상세 조회",
            description = "게시된 적이 있는 회차의 공개 정보를 반환합니다. 초안과 일정 미설정 회차는 404입니다.")
    @GetMapping("/{recruitmentId}")
    public ResponseEntity<ApiResponse<StaffRecruitmentResponse>> getDetail(
            @Parameter(description = "모집 회차 ID") @PathVariable Integer recruitmentId
    ) {
        StaffRecruitmentResponse response =
                StaffRecruitmentResponse.from(staffRecruitmentService.getPublicDetail(recruitmentId));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(ApiResponse.success(response));
    }
}
