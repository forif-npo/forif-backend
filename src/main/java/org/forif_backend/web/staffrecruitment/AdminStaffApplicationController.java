package org.forif_backend.web.staffrecruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.staffrecruitment.StaffApplicationService;
import org.forif_backend.application.staffrecruitment.dto.StaffApplicationSummary;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffApplicationResponse.DetailResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffApplicationResponse.SummaryResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 회장단의 제출 지원서 조회.
 *
 * 4단계에서 회차별 열람 권한(reviewer)을 추가할 때 이 컨트롤러의 권한 검사 지점만 확장한다.
 * 지금은 회장단만 열람할 수 있다.
 */
@Tag(name = "운영진 지원 관리 (회장단)", description = "제출된 운영진 지원서 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/staff-recruitments/{recruitmentId}/applications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffApplicationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final StaffApplicationService staffApplicationService;
    private final StaffAccountService staffAccountService;

    @Operation(summary = "지원서 목록",
            description = "이름 검색과 지망 부서 필터를 지원합니다. 연락처와 답변은 상세에서만 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<SummaryResponse>>> getList(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Parameter(description = "지망 부서 ID (1·2지망 모두 일치)") @RequestParam(required = false) Integer departmentId,
            @Parameter(description = "제출 당시 이름 검색") @RequestParam(required = false) String search,
            @Parameter(description = "커서 (이전 응답의 next_cursor)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "0부터 시작하는 페이지. 주면 오프셋 방식") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기 (최대 100)") @RequestParam(defaultValue = "20") int size
    ) {
        staffAccountService.requirePresidentTeam(userId);

        CursorPageResponse<StaffApplicationSummary> result =
                staffApplicationService.getAdminList(recruitmentId, departmentId, search, cursor, page,
                        clampSize(size));

        return noStore(ApiResponse.success(
                result.withContent(result.content().stream().map(SummaryResponse::from).toList())));
    }

    @Operation(summary = "지원서 상세", description = "제출 당시 연락처와 전체 답변을 반환합니다.")
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<DetailResponse>> getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @PathVariable Integer applicationId
    ) {
        staffAccountService.requirePresidentTeam(userId);

        return noStore(ApiResponse.success(DetailResponse.from(
                staffApplicationService.getAdminDetail(recruitmentId, applicationId))));
    }

    private int clampSize(int size) {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /** 개인정보가 포함되므로 캐시하지 않는다. */
    private <T> ResponseEntity<ApiResponse<T>> noStore(ApiResponse<T> body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(body);
    }
}
