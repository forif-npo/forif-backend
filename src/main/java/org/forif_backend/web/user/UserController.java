package org.forif_backend.web.user;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.user.UserService;
import org.forif_backend.application.user.dto.*;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.user.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.user.dto.StudyApplyRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    /**
     * 부원 회원가입
     * 프론트엔드에서 Google OAuth로 획득한 이메일을 함께 전송
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserSignUpResponse>> userSignUp(
            @RequestBody UserSignUpRequest request
    ) {
        // 1. Web DTO → Application Command 변환
        UserSignUpCommand command = UserDtoMapper.toCommand(request);

        // 2. Service 호출
        UserSignUpResult result = userService.userSignUp(command);

        // 3. Application Result → Web DTO 변환
        UserSignUpResponse response = UserDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 부원 로그인
     */
    @PostMapping("/signin")
    public ResponseEntity<ApiResponse<UserSignInResponse>> userSignIn(@RequestBody UserSignInRequest request) {
        // 1. Google에서 이메일 가져오기
        String email = userService.getEmailFromGoogleToken(request.accessToken());

        // 2. Web DTO → Application Command 변환
        UserSignInCommand command = UserDtoMapper.toCommand(email);

        // 3. Service 호출
        UserSignInResult result = userService.userSignIn(command);

        // 4. Application Result → Web DTO 변환
        UserSignInResponse response = UserDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Refresh Token으로 Access Token 재발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refreshAccessToken(@RequestBody RefreshTokenRequest request) {
        // 1. Web DTO → Application Command 변환
        RefreshTokenCommand command = UserDtoMapper.toCommand(request);

        // 2. Service 호출
        RefreshTokenResult result = userService.refreshAccessToken(command);

        // 3. Application Result → Web DTO 변환
        AccessTokenResponse response = UserDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/study")
    public ResponseEntity<ApiResponse<Void>> applyStudy(@AuthenticationPrincipal Long userId,
                                                       @Valid @RequestBody StudyApplyRequest studyApplyRequest) {
        userService.applyStudy(userId, studyApplyRequest);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }
}
