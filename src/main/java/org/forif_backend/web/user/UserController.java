package org.forif_backend.web.user;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.auth.TokenBlacklistService;
import org.forif_backend.application.user.UserService;
import org.forif_backend.application.user.dto.*;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.user.dto.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProvider jwtProvider;

    /**
     * 부원 회원가입
     * 프론트엔드에서 Google OAuth로 획득한 이메일을 함께 전송
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserSignUpResponse>> userSignUp(
            @RequestBody UserSignUpRequest request,
            HttpServletResponse httpResponse
    ) {
        // 1. Web DTO → Application Command 변환
        UserSignUpCommand command = UserDtoMapper.toCommand(request);

        // 2. Service 호출
        UserSignUpResult result = userService.userSignUp(command);

        // 3. Refresh Token을 HttpOnly 쿠키로 설정
        Cookie refreshTokenCookie = new Cookie("refreshToken", result.refreshToken());
        refreshTokenCookie.setHttpOnly(true);  // JavaScript에서 접근 불가
        refreshTokenCookie.setSecure(true);    // HTTPS에서만 전송
        refreshTokenCookie.setPath("/");       // 모든 경로에서 사용 가능
        refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60); // 30일
        // refreshTokenCookie.setAttribute("SameSite", "Strict"); // CSRF 방지
        httpResponse.addCookie(refreshTokenCookie);

        // 4. Application Result → Web DTO 변환 (refreshToken 제외)
        UserSignUpResponse response = UserDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 부원 로그인
     */
    @PostMapping("/signin")
    public ResponseEntity<ApiResponse<UserSignInResponse>> userSignIn(
            @RequestBody UserSignInRequest request,
            HttpServletResponse httpResponse
    ) {
        // 1. Google에서 이메일 가져오기
        String email = userService.getEmailFromGoogleToken(request.accessToken());

        // 2. Web DTO → Application Command 변환
        UserSignInCommand command = UserDtoMapper.toCommand(email);

        // 3. Service 호출
        UserSignInResult result = userService.userSignIn(command);

        // 4. Refresh Token을 HttpOnly 쿠키로 설정
        Cookie refreshTokenCookie = new Cookie("refreshToken", result.refreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60); // 30일
        httpResponse.addCookie(refreshTokenCookie);

        // 5. Application Result → Web DTO 변환 (refreshToken 제외)
        UserSignInResponse response = UserDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Refresh Token으로 Access Token 재발급 (토큰 로테이션 적용)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpResponse
    ) {
        // 1. 쿠키에서 Refresh Token 확인
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("FOR013-401", "Refresh Token이 없습니다."));
        }

        // 2. Web DTO → Application Command 변환
        RefreshTokenCommand command = new RefreshTokenCommand(refreshToken);

        // 3. Service 호출 (로테이션: 새 Access Token + 새 Refresh Token 발급)
        RefreshTokenResult result = userService.refreshAccessToken(command);

        // 4. 새 Refresh Token을 HttpOnly 쿠키로 설정
        Cookie newRefreshTokenCookie = new Cookie("refreshToken", result.refreshToken());
        newRefreshTokenCookie.setHttpOnly(true);
        newRefreshTokenCookie.setSecure(true);
        newRefreshTokenCookie.setPath("/");
        newRefreshTokenCookie.setMaxAge(30 * 24 * 60 * 60); // 30일
        httpResponse.addCookie(newRefreshTokenCookie);

        // 5. Application Result → Web DTO 변환 (Access Token만 응답)
        AccessTokenResponse response = UserDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 로그아웃 (Access Token 블랙리스트 등록 + Refresh Token 삭제)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            HttpServletRequest request,
            HttpServletResponse httpResponse
    ) {
        // 1. Authorization Header에서 Access Token 추출
        String accessToken = extractTokenFromRequest(request);

        // 2. Access Token이 있으면 블랙리스트에 추가
        if (accessToken != null) {
            tokenBlacklistService.blacklistToken(accessToken);

            // 3. 토큰에서 사용자 ID 추출하여 Refresh Token 삭제
            String userId = jwtProvider.getUserIdFromToken(accessToken);
            refreshTokenService.deleteRefreshToken(userId);
        }

        // 4. Refresh Token 쿠키 삭제
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0); // 즉시 만료
        httpResponse.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(ApiResponse.successWithMsg("로그아웃되었습니다."));
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
    @PostMapping("/study")
    public ResponseEntity<ApiResponse<Void>> applyStudy(@AuthenticationPrincipal Long userId,
                                                       @Valid @RequestBody StudyApplyRequest studyApplyRequest) {
        userService.applyStudy(userId, studyApplyRequest);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }
}
