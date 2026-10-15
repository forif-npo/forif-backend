package org.forif_backend.web.staffrecruitment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.staffrecruitment.StaffReviewerService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffReviewerResponse.CandidateResponse;
import org.forif_backend.web.staffrecruitment.dto.StaffReviewerResponse.GrantRequest;
import org.forif_backend.web.staffrecruitment.dto.StaffReviewerResponse.ReviewerResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.List;

/**
 * 회차별 지원서 열람 권한 관리. 회장단만 부여·회수할 수 있다.
 * 열람자는 다른 사람을 초대하거나 모집 설정을 바꿀 수 없다.
 */
@Tag(name = "운영진 지원 열람 권한 (회장단)", description = "회차별 reviewer 후보 조회·부여·회수")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/staff-recruitments/{recruitmentId}")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffReviewerController {

    private static final ZoneId KOREA_STANDARD_TIME = ZoneId.of("Asia/Seoul");

    private final StaffReviewerService staffReviewerService;
    private final StaffAccountService staffAccountService;

    @Operation(summary = "열람자 초대 후보",
            description = "회차 학기의 운영진 명단입니다. ADMIN 계정이 없는 사람은 부여할 수 없습니다.")
    @GetMapping("/reviewer-candidates")
    public ResponseEntity<ApiResponse<List<CandidateResponse>>> getCandidates(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "모집 회차 ID") @PathVariable Integer recruitmentId
    ) {
        staffAccountService.requirePresidentTeam(userId);

        List<CandidateResponse> response = staffReviewerService.getCandidates(recruitmentId).stream()
                .map(CandidateResponse::from)
                .toList();

        return noStore(ApiResponse.success(response));
    }

    @Operation(summary = "열람 권한 목록", description = "회수·만료된 권한까지 이력으로 함께 반환합니다.")
    @GetMapping("/reviewers")
    public ResponseEntity<ApiResponse<List<ReviewerResponse>>> getReviewers(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId
    ) {
        staffAccountService.requirePresidentTeam(userId);

        List<ReviewerResponse> response = staffReviewerService.getReviewers(recruitmentId).stream()
                .map(ReviewerResponse::from)
                .toList();

        return noStore(ApiResponse.success(response));
    }

    @Operation(summary = "열람 권한 부여",
            description = "즉시 부여됩니다. 같은 회차 지원자와 ADMIN 계정이 없는 운영진은 부여할 수 없습니다.")
    @PostMapping("/reviewers")
    public ResponseEntity<ApiResponse<ReviewerResponse>> grant(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Valid @RequestBody GrantRequest request
    ) {
        staffAccountService.requirePresidentTeam(userId);

        ReviewerResponse response = ReviewerResponse.from(staffReviewerService.grant(
                recruitmentId, userId, request.userId(),
                request.expiresAt().atZoneSameInstant(KOREA_STANDARD_TIME).toLocalDateTime()));

        return noStore(ApiResponse.success(response));
    }

    @Operation(summary = "열람 권한 회수",
            description = "회수 후 다음 요청부터 바로 거절됩니다. 여러 번 호출해도 결과가 같습니다.")
    @DeleteMapping("/reviewers/{targetUserId}")
    public ResponseEntity<Void> revoke(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer recruitmentId,
            @Parameter(description = "권한을 회수할 회원 ID") @PathVariable Long targetUserId
    ) {
        staffAccountService.requirePresidentTeam(userId);
        staffReviewerService.revoke(recruitmentId, userId, targetUserId);

        return ResponseEntity.noContent().build();
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(ApiResponse<T> body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(body);
    }
}
