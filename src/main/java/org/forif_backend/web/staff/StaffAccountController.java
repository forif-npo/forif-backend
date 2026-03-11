package org.forif_backend.web.staff;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.staff.dto.CreateAdminCommand;
import org.forif_backend.application.staff.dto.CreateMentorCommand;
import org.forif_backend.application.staff.dto.StaffSignInCommand;
import org.forif_backend.application.staff.dto.StaffSignInResult;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.web.staff.dto.*;
import org.forif_backend.common.util.CookieUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "스태프", description = "멘토 및 운영진 계정 관리 API")
@Slf4j
@RestController
@RequiredArgsConstructor
public class StaffAccountController {

    private final StaffAccountService staffAccountService;

    /**
     * 스태프(멘토/운영진) 로그인
     */
    @Operation(summary = "스태프 로그인", description = "멘토 또는 운영진 계정으로 로그인합니다. Refresh Token은 HttpOnly 쿠키로 발급됩니다.")
    @PostMapping("/api/v1/staff/signin")
    public ResponseEntity<ApiResponse<StaffSignInResponse>> staffSignIn(
            @RequestBody StaffSignInRequest request,
            HttpServletResponse httpResponse
    ) {
        StaffSignInCommand command = StaffDtoMapper.toCommand(request);
        StaffSignInResult result = staffAccountService.staffSignIn(command);

        CookieUtils.addRefreshTokenCookie(httpResponse, result.refreshToken());

        StaffSignInResponse response = StaffDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 현재 로그인한 스태프 정보 조회
     */
    @Operation(summary = "내 스태프 정보 조회", description = "현재 로그인한 스태프의 이름, 역할, 소속 정보를 조회합니다.")
    @GetMapping("/api/v1/staff/me")
    public ResponseEntity<ApiResponse<StaffInfoResponse>> getStaffInfo(
            @AuthenticationPrincipal Long userId
    ) {
        StaffAccount staffAccount = staffAccountService.getStaffInfo(userId);
        StaffInfoResponse response = StaffDtoMapper.toResponse(staffAccount);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== 회장단 운영진 관리 API ====================

    /**
     * [회장단 전용] 운영진 목록 조회 (커서 페이지네이션)
     */
    @Operation(summary = "운영진 목록 조회 (회장단 전용)", description = "커서 기반 페이지네이션으로 운영진 목록을 조회합니다.")
    @GetMapping("/api/v1/president/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminResponse>>> getAdmins(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "이전 페이지의 마지막 운영진 ID. 최초 조회 시 생략") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "이름 검색어") @RequestParam(required = false) String search
    ) {
        CursorPageResponse<StaffAccount> result = staffAccountService.getAdmins(userId, cursor, size, search);

        List<AdminResponse> content = result.content().stream()
                .map(StaffDtoMapper::toAdminResponse)
                .toList();

        CursorPageResponse<AdminResponse> response = new CursorPageResponse<>(
                content, result.nextCursor(), result.hasNext(), result.totalElements()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * [회장단 전용] 운영진 계정 생성
     */
    @Operation(summary = "운영진 계정 생성 (회장단 전용)", description = "새 운영진 계정을 생성합니다.")
    @PostMapping("/api/v1/president/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminResponse>> createAdmin(
            @AuthenticationPrincipal Long userId,
            @RequestBody CreateAdminRequest request
    ) {
        CreateAdminCommand command = StaffDtoMapper.toCommand(request);
        StaffAccount staffAccount = staffAccountService.createAdmin(userId, command);
        AdminResponse response = StaffDtoMapper.toAdminResponse(staffAccount);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * [회장단 전용] 운영진 정보 수정
     */
    @Operation(summary = "운영진 정보 수정 (회장단 전용)", description = "운영진의 이름, 비밀번호, 소속을 수정합니다.")
    @PatchMapping("/api/v1/president/admins/{targetUserId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminResponse>> updateAdmin(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "수정할 운영진의 유저 ID") @PathVariable Long targetUserId,
            @RequestBody UpdateAdminRequest request
    ) {
        StaffAccount staffAccount = staffAccountService.updateAdmin(
                userId, targetUserId, request.name(), request.password(), request.affiliation()
        );
        AdminResponse response = StaffDtoMapper.toAdminResponse(staffAccount);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * [회장단 전용] 운영진 계정 삭제
     */
    @Operation(summary = "운영진 계정 삭제 (회장단 전용)", description = "운영진 계정을 삭제합니다.")
    @DeleteMapping("/api/v1/president/admins/{targetUserId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "삭제할 운영진의 유저 ID") @PathVariable Long targetUserId
    ) {
        staffAccountService.deleteAdmin(userId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * [회장 전용] 회장/부회장 위임
     */
    @Operation(summary = "회장/부회장 위임 (회장 전용)", description = "회장 또는 부회장 권한을 다른 운영진에게 위임합니다.")
    @PostMapping("/api/v1/president/delegate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delegate(
            @AuthenticationPrincipal Long userId,
            @RequestBody DelegateRequest request
    ) {
        staffAccountService.delegate(userId, request.userId(), request.affiliation());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== 어드민 멘토 관리 API ====================

    /**
     * 멘토 목록 조회 (운영진 전용, 커서 페이지네이션)
     */
    @Operation(summary = "멘토 목록 조회 (어드민 전용)", description = "커서 기반 페이지네이션으로 멘토 목록을 조회합니다.")
    @GetMapping("/api/v1/admin/mentors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CursorPageResponse<MentorResponse>>> getMentors(
            @Parameter(description = "이전 페이지의 마지막 멘토 ID. 최초 조회 시 생략") @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "이름 검색어") @RequestParam(required = false) String search
    ) {
        CursorPageResponse<StaffAccount> result = staffAccountService.getMentors(cursor, size, search);

        List<MentorResponse> content = result.content().stream()
                .map(MentorResponse::from)
                .toList();

        CursorPageResponse<MentorResponse> response = new CursorPageResponse<>(
                content, result.nextCursor(), result.hasNext(), result.totalElements()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 멘토 계정 생성 (운영진 전용)
     */
    @Operation(summary = "멘토 계정 생성 (어드민 전용)", description = "새 멘토 계정을 수동으로 생성합니다. 스터디 승인 시 자동 생성도 지원됩니다.")
    @PostMapping("/api/v1/admin/mentors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> createMentor(
            @Valid @RequestBody CreateMentorRequest request
    ) {
        CreateMentorCommand command = StaffDtoMapper.toCommand(request);
        staffAccountService.createMentorAccount(command);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 멘토 정보 수정 (운영진 전용)
     */
    @Operation(summary = "멘토 정보 수정 (어드민 전용)", description = "멘토의 이름, 비밀번호, 소속 스터디를 수정합니다.")
    @PatchMapping("/api/v1/admin/mentors/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateMentor(
            @Parameter(description = "수정할 멘토의 유저 ID") @PathVariable Long userId,
            @RequestBody UpdateMentorRequest request
    ) {
        staffAccountService.updateMentorAccount(userId, request.name(), request.password(), request.affiliation());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 멘토 계정 삭제 (운영진 전용)
     */
    @Operation(summary = "멘토 계정 삭제 (어드민 전용)", description = "멘토 계정을 삭제합니다.")
    @DeleteMapping("/api/v1/admin/mentors/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMentor(
            @Parameter(description = "삭제할 멘토의 유저 ID") @PathVariable Long userId
    ) {
        staffAccountService.deleteMentorAccount(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
