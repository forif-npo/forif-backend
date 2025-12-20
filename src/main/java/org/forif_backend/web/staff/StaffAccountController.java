package org.forif_backend.web.staff;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.staff.dto.StaffSignInCommand;
import org.forif_backend.application.staff.dto.StaffSignInResult;
import org.forif_backend.application.staff.dto.StaffSignUpCommand;
import org.forif_backend.application.staff.dto.StaffSignUpResult;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.staff.dto.StaffSignInRequest;
import org.forif_backend.web.staff.dto.StaffSignInResponse;
import org.forif_backend.web.staff.dto.StaffSignupRequest;
import org.forif_backend.web.staff.dto.StaffSignupResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff")
public class StaffAccountController {

    private final StaffAccountService staffAccountService;

    /**
     * 스태프(멘토/운영진) 로그인
     */
    @PostMapping("/signin")
    public ResponseEntity<ApiResponse<StaffSignInResponse>> staffSignIn(
            @RequestBody StaffSignInRequest request,
            HttpServletResponse httpResponse
    ) {
        // 1. Web DTO → Application Command 변환
        StaffSignInCommand command = StaffDtoMapper.toCommand(request);

        // 2. Service 호출
        StaffSignInResult result = staffAccountService.staffSignIn(command);

        // 3. Refresh Token을 HttpOnly 쿠키로 설정
        Cookie refreshTokenCookie = new Cookie("refreshToken", result.refreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60); // 30일
        httpResponse.addCookie(refreshTokenCookie);

        // 4. Application Result → Web DTO 변환 (refreshToken 제외)
        StaffSignInResponse response = StaffDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 스태프(멘토/ 운영진) 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<StaffSignupResponse>> staffSignUp(
            @RequestBody StaffSignupRequest request,
            HttpServletResponse httpResponse
    ) {
        // 1. Web DTO → Application Command 변환
        StaffSignUpCommand command = StaffDtoMapper.toCommand(request);

        // 2. Service 호출
        StaffSignUpResult result = staffAccountService.staffSignUp(command);

        log.info("result: {}", result);
        // 3. Refresh Token을 HttpOnly 쿠키로 설정
        Cookie refreshTokenCookie = new Cookie("refreshToken", result.refreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60); // 30일
        httpResponse.addCookie(refreshTokenCookie);

        // 4. Application Result → Web DTO 변환 (refreshToken 제외)
        StaffSignupResponse response = StaffDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}