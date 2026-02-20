package org.forif_backend.web.staff;

import jakarta.servlet.http.Cookie;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StaffAccountController {

    private final StaffAccountService staffAccountService;

    /**
     * 스태프(멘토/운영진) 로그인
     */
    @PostMapping("/api/v1/staff/signin")
    public ResponseEntity<ApiResponse<StaffSignInResponse>> staffSignIn(
            @RequestBody StaffSignInRequest request,
            HttpServletResponse httpResponse
    ) {
        StaffSignInCommand command = StaffDtoMapper.toCommand(request);
        StaffSignInResult result = staffAccountService.staffSignIn(command);

        Cookie refreshTokenCookie = new Cookie("refreshToken", result.refreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60); // 30일
        httpResponse.addCookie(refreshTokenCookie);

        StaffSignInResponse response = StaffDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 현재 로그인한 스태프 정보 조회
     */
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
    @GetMapping("/api/v1/president/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminResponse>>> getAdmins(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
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
    @PatchMapping("/api/v1/president/admins/{targetUserId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminResponse>> updateAdmin(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long targetUserId,
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
    @DeleteMapping("/api/v1/president/admins/{targetUserId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long targetUserId
    ) {
        staffAccountService.deleteAdmin(userId, targetUserId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * [회장 전용] 회장/부회장 위임
     */
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
    @GetMapping("/api/v1/admin/mentors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CursorPageResponse<MentorResponse>>> getMentors(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
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
    @PatchMapping("/api/v1/admin/mentors/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateMentor(
            @PathVariable Long userId,
            @RequestBody UpdateMentorRequest request
    ) {
        staffAccountService.updateMentorAccount(userId, request.name(), request.password(), request.affiliation());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 멘토 계정 삭제 (운영진 전용)
     */
    @DeleteMapping("/api/v1/admin/mentors/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMentor(
            @PathVariable Long userId
    ) {
        staffAccountService.deleteMentorAccount(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
