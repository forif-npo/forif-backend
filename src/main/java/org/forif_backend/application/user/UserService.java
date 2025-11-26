package org.forif_backend.application.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.user.dto.*;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.user.GoogleOAuthClient;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final GoogleOAuthClient googleOAuthClient;
    private final RefreshTokenService refreshTokenService;

    /**
     * 부원 회원가입
     */
    @Transactional
    public UserSignUpResult userSignUp(UserSignUpCommand command) {
        // 1. 한양대 이메일 도메인 검증
        if (!command.email().endsWith("@hanyang.ac.kr")) {
            throw new ForifException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }

        // 2. 중복 확인
        if (userRepository.existsById(command.studentId())) {
            throw new ForifException(ErrorCode.STUDENT_ID_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new ForifException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 3. 사용자 생성
        User user = User.createUser(
                command.studentId(),
                command.userName(),
                command.email(),
                command.phoneNum(),
                command.department()
        );

        User savedUser = userRepository.save(user);

        // 4. JWT 토큰 생성
        String role = "USER";
        String userId = savedUser.getId().toString();
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        // 5. Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(userId, refreshToken);

        return new UserSignUpResult(
                accessToken,
                refreshToken,
                role
        );
    }


    /**
     * 부원 로그인
     */
    public UserSignInResult userSignIn(UserSignInCommand command) {
        // 1. 한양대 이메일 도메인 검증
        if (!command.email().endsWith("@hanyang.ac.kr")) {
            throw new ForifException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }


        // 2. 기존 사용자 조회
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        // 3. JWT 토큰 생성
        String role = "USER";
        String userId = user.getId().toString();
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        // 4. Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(userId, refreshToken);


        return new UserSignInResult(
                accessToken,
                refreshToken,
                role
        );
    }


    /**
     * Google OAuth Access Token으로 사용자 이메일 조회
     */
    public String getEmailFromGoogleToken(String token) {
        return googleOAuthClient.getEmailFromToken(token);
    }

    /**
     * Refresh Token으로 새로운 Access Token 발급 (토큰 로테이션 적용)
     */
    public RefreshTokenResult refreshAccessToken(RefreshTokenCommand command) {
        // 1. 토큰에서 role 추출 (로테이션 전)
        String role = jwtProvider.getRoleFromToken(command.refreshToken());

        // 2. Refresh Token 로테이션 (기존 토큰 무효화 + 새 토큰 발급)
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotateRefreshToken(command.refreshToken(), role);

        return new RefreshTokenResult(tokenPair.accessToken(), tokenPair.refreshToken());
    }
}
