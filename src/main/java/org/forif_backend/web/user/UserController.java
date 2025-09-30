package org.forif_backend.web.user;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.user.UserService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.user.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    /**
     * 부원 회원가입 
     */
    @PostMapping("/member/signup")
    public ResponseEntity<ApiResponse<MemberSignUpResponse>> memberSignUp(
            @RequestHeader("Authorization") String googleAccessToken,
            @RequestBody MemberSignUpRequest request
    ) {
        MemberSignUpResponse response = userService.memberSignUp(request, googleAccessToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 부원 로그인 
     */
    @PostMapping("/member/signin")
    public ResponseEntity<ApiResponse<SignInResponse>> memberSignIn(@RequestBody MemberSignInRequest request) {
        SignInResponse response = userService.memberSignIn(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 멘토 로그인 (ID/비밀번호) 
     */
    @PostMapping("/mentor/signin")
    public ResponseEntity<ApiResponse<SignInResponse>> mentorSignIn(@RequestBody MentorSignInRequest request) {
        SignInResponse response = userService.mentorSignIn(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Refresh Token으로 Access Token 재발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refreshAccessToken(@RequestBody RefreshTokenRequest request) {
        AccessTokenResponse response = userService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Google 토큰으로 사용자 정보 조회 (회원가입 폼용)
     */
    @PostMapping("/google/userinfo")
    public ResponseEntity<ApiResponse<GoogleUserInfo>> getGoogleUserInfo(
            @RequestHeader("Authorization") String googleAccessToken
    ) {
        GoogleUserInfo response = userService.getGoogleUserInfo(googleAccessToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 멘토 계정 생성 API 구현 필요
}
