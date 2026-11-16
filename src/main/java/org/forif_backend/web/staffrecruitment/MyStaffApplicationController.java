package org.forif_backend.web.staffrecruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staffrecruitment.StaffApplicationService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffApplicationResponse.MySummaryResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "운영진 지원 (지원자)", description = "지원서 제출과 본인 지원 내역 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff-applications")
public class MyStaffApplicationController {

    private final StaffApplicationService staffApplicationService;

    @Operation(summary = "내 운영진 지원 이력 조회", description = "로그인한 회원의 전체 회차 지원 이력을 최신순으로 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<MySummaryResponse>>> getMyHistory(
            @AuthenticationPrincipal Long userId
    ) {
        List<MySummaryResponse> history = staffApplicationService.getMyHistory(userId).stream()
                .map(MySummaryResponse::from)
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(history));
    }
}
