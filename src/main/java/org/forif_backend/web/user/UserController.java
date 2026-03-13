package org.forif_backend.web.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.auth.TokenBlacklistService;
import org.forif_backend.application.study.StudyService;
import org.forif_backend.application.study.dto.UserStudiesResult;
import org.forif_backend.application.user.UserApplyService;
import org.forif_backend.application.user.UserService;
import org.forif_backend.application.user.dto.*;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.domain.user.User;
import org.forif_backend.web.user.dto.*;
import org.forif_backend.common.util.CookieUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "부원", description = "부원 인증 및 마이페이지 API")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProvider jwtProvider;
    private final UserApplyService userApplyService;
    private final StudyService studyService;

    /**
     * 부원 회원가입
     * 프론트엔드에서 Google OAuth로 획득한 이메일을 함께 전송
     */
    @Operation(summary = "부원 회원가입", description = "Google OAuth 이메일 인증 후 신규 부원을 등록합니다. Refresh Token은 HttpOnly 쿠키로 발급됩니다.")
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
        CookieUtils.addRefreshTokenCookie(httpResponse, result.refreshToken());

        // 4. Application Result → Web DTO 변환 (refreshToken 제외)
        UserSignUpResponse response = UserDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 부원 로그인
     */
    @Operation(summary = "부원 로그인", description = "Google OAuth Access Token으로 로그인합니다. Refresh Token은 HttpOnly 쿠키로 발급됩니다.")
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
        CookieUtils.addRefreshTokenCookie(httpResponse, result.refreshToken());

        // 5. Application Result → Web DTO 변환 (refreshToken 제외)
        UserSignInResponse response = UserDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Refresh Token으로 Access Token 재발급 (토큰 로테이션 적용)
     */
    @Operation(summary = "Access Token 재발급", description = "HttpOnly 쿠키의 Refresh Token으로 새 Access Token을 발급합니다. 토큰 로테이션이 적용됩니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpResponse
    ) {
        // 1. 쿠키에서 Refresh Token 확인
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(ErrorCode.INVALID_TOKEN));
        }

        // 2. Web DTO → Application Command 변환
        RefreshTokenCommand command = new RefreshTokenCommand(refreshToken);

        // 3. Service 호출 (로테이션: 새 Access Token + 새 Refresh Token 발급)
        RefreshTokenResult result = userService.refreshAccessToken(command);

        // 4. 새 Refresh Token을 HttpOnly 쿠키로 설정
        CookieUtils.addRefreshTokenCookie(httpResponse, result.refreshToken());

        // 5. Application Result → Web DTO 변환 (Access Token만 응답)
        AccessTokenResponse response = UserDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 로그아웃 (Access Token 블랙리스트 등록 + Refresh Token 삭제)
     */
    @Operation(summary = "로그아웃", description = "Access Token을 블랙리스트에 등록하고 Refresh Token을 삭제합니다.")
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
        CookieUtils.deleteRefreshTokenCookie(httpResponse);

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

    /**
     * 수강 스터디 조회
     * 부원이 현재 수강중인 스터디와 역대 수강한 스터디를 학기별로 그룹화하여 반환
     */
    @Operation(summary = "내 수강 스터디 조회", description = "로그인한 부원이 수강 중이거나 과거에 수강한 스터디를 학기별로 그룹화하여 반환합니다.")
    @GetMapping("/me/studies")
    public ResponseEntity<ApiResponse<UserStudiesResponse>> getUserStudies(@AuthenticationPrincipal Long userId) {
        // 1. Service 호출
        UserStudiesResult result = studyService.getUserStudies(userId);

        // 2. Application Result → Web DTO 변환
        UserStudiesResponse response = UserDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 멘티 스터디 신청서 목록 조회
     */
    @Operation(summary = "내 스터디 수강 신청서 목록 조회", description = "로그인한 부원이 제출한 스터디 수강 신청서 목록을 조회합니다.")
    @GetMapping("/me/study-applications")
    public ResponseEntity<ApiResponse<StudyApplicationsResponse>> getStudyApplications(
            @AuthenticationPrincipal Long userId
    ) {
        GetStudyApplicationsResult result = userService.getStudyApplications(userId);
        StudyApplicationsResponse response = UserDtoMapper.toResponse(result);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 멘토 스터디 개설 신청서 목록 조회
     */
    @Operation(summary = "내 스터디 개설 신청서 목록 조회 (멘토 전용)", description = "로그인한 멘토가 제출한 스터디 개설 신청서 목록과 심사 상태를 조회합니다.")
    @GetMapping("/me/study-creation-applications")
    public ResponseEntity<ApiResponse<StudyCreationApplicationsResponse>> getStudyCreationApplications(
            @AuthenticationPrincipal Long userId
    ) {
        GetStudyCreationApplicationsResult result = userService.getStudyCreationApplications(userId);
        StudyCreationApplicationsResponse response = UserDtoMapper.toResponse(result);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 인증서 조회
     * 특정 스터디에 대한 본인의 인증서 URL을 조회
     */
    @Operation(summary = "수료증 조회", description = "특정 스터디에 대한 본인의 수료증 URL을 조회합니다.")
    @GetMapping("/me/certificates")
    public ResponseEntity<ApiResponse<CertificateResponse>> getCertificate(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "수료증을 조회할 스터디 ID") @RequestParam("studyId") Integer studyId
    ) {
        GetCertificateResult result = userService.getCertificate(userId, studyId);
        CertificateResponse response = UserDtoMapper.toResponse(result);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Operation(summary = "내 정보 조회", description = "로그인한 부원의 이름, 이메일, 학과 등 기본 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserInfo(
            @AuthenticationPrincipal Long userId
    ) {
        User user = userService.getUserInfo(userId);
        UserInfoResponse response = UserDtoMapper.toResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
