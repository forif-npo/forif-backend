package org.forif_backend.web.staffrecruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.staffrecruitment.StaffRecruitmentService;
import org.forif_backend.application.staffrecruitment.StaffReviewerService;
import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentDetail;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentStatus;
import org.forif_backend.web.staffrecruitment.dto.AdminStaffRecruitmentResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffRecruitmentRequests.ActionRequest;
import org.forif_backend.web.staffrecruitment.dto.StaffRecruitmentRequests.CreateRequest;
import org.forif_backend.web.staffrecruitment.dto.StaffRecruitmentRequests.UpdateRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 회장단 전용 운영진 모집 관리.
 * 일반 ADMIN은 진입할 수 없도록 기존 회장단 검증을 그대로 사용한다.
 */
@Tag(name = "운영진 모집 관리 (회장단)", description = "모집 회차 생성·수정·게시·마감·취소 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/staff-recruitments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffRecruitmentController {

    private static final int MAX_PAGE_SIZE = 100;

    private final StaffRecruitmentService staffRecruitmentService;
    private final StaffReviewerService staffReviewerService;
    private final StaffAccountService staffAccountService;

    @Operation(summary = "모집 회차 목록", description = "초안을 포함한 전체 목록입니다. 정렬은 모집 회차 ID 내림차순 고정입니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminStaffRecruitmentResponse>>> getList(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "저장 상태 필터") @RequestParam(required = false) StaffRecruitmentStatus status,
            @Parameter(description = "0부터 시작하는 페이지") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 100)") @RequestParam(defaultValue = "20") int size
    ) {
        staffAccountService.requirePresidentTeam(userId);

        CursorPageResponse<StaffRecruitmentDetail> result =
                staffRecruitmentService.getAdminList(status, Math.max(page, 0), clampSize(size));

        return adminResponse(ApiResponse.success(
                result.withContent(result.content().stream().map(AdminStaffRecruitmentResponse::from).toList())));
    }

    @Operation(summary = "열람 가능한 모집 회차",
            description = "회장단은 전체 회차를, 회차별 열람자는 권한이 유효한 회차만 받습니다.")
    @GetMapping("/reviewable")
    public ResponseEntity<ApiResponse<List<AdminStaffRecruitmentResponse>>> getReviewable(
            @AuthenticationPrincipal Long userId
    ) {
        List<StaffRecruitmentDetail> details = staffAccountService.isPresidentTeam(userId)
                ? staffRecruitmentService.getAdminList(null, 0, MAX_PAGE_SIZE).content()
                : staffRecruitmentService.getByIds(staffReviewerService.getReviewableRecruitmentIds(userId));

        return adminResponse(ApiResponse.success(
                details.stream().map(AdminStaffRecruitmentResponse::from).toList()));
    }

    @Operation(summary = "모집 회차 생성", description = "초안 상태로 만듭니다. 게시는 별도 요청입니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminStaffRecruitmentResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);

        AdminStaffRecruitmentResponse response =
                AdminStaffRecruitmentResponse.from(staffRecruitmentService.create(request.toCommand()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "모집 회차 상세", description = "초안과 비활성 부서까지 포함한 관리용 상세입니다.")
    @GetMapping("/{recruitmentId}")
    public ResponseEntity<ApiResponse<AdminStaffRecruitmentResponse>> getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return adminResponse(ApiResponse.success(
                AdminStaffRecruitmentResponse.from(staffRecruitmentService.getForAdmin(recruitmentId))));
    }

    @Operation(summary = "모집 회차 수정",
            description = "마감·취소된 회차는 수정할 수 없습니다. version이 다르면 409로 거절합니다.")
    @PatchMapping("/{recruitmentId}")
    public ResponseEntity<ApiResponse<AdminStaffRecruitmentResponse>> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Valid @RequestBody UpdateRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return adminResponse(ApiResponse.success(AdminStaffRecruitmentResponse.from(
                staffRecruitmentService.update(recruitmentId, request.toCommand()))));
    }

    @Operation(summary = "모집 회차 게시",
            description = "초안만 게시할 수 있고 기간이 설정돼 있어야 합니다. 이미 게시 중인 회차가 있으면 409입니다.")
    @PostMapping("/{recruitmentId}/publish")
    public ResponseEntity<ApiResponse<AdminStaffRecruitmentResponse>> publish(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Valid @RequestBody ActionRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return adminResponse(ApiResponse.success(AdminStaffRecruitmentResponse.from(
                staffRecruitmentService.publish(recruitmentId, request.version()))));
    }

    @Operation(summary = "모집 회차 마감",
            description = "게시 중인 회차를 마감합니다. 종료 시각 이후의 마감은 자연 종료로 기록합니다.")
    @PostMapping("/{recruitmentId}/close")
    public ResponseEntity<ApiResponse<AdminStaffRecruitmentResponse>> close(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Valid @RequestBody ActionRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return adminResponse(ApiResponse.success(AdminStaffRecruitmentResponse.from(
                staffRecruitmentService.close(recruitmentId, request.reason(), request.version()))));
    }

    @Operation(summary = "모집 회차 취소", description = "초안과 게시 중인 회차를 취소합니다. 취소 시각을 기록합니다.")
    @PostMapping("/{recruitmentId}/cancel")
    public ResponseEntity<ApiResponse<AdminStaffRecruitmentResponse>> cancel(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Valid @RequestBody ActionRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);
        return adminResponse(ApiResponse.success(AdminStaffRecruitmentResponse.from(
                staffRecruitmentService.cancel(recruitmentId, request.reason(), request.version()))));
    }

    private int clampSize(int size) {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /** 관리 응답은 캐시하지 않는다. */
    private <T> ResponseEntity<ApiResponse<T>> adminResponse(ApiResponse<T> body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(body);
    }
}
