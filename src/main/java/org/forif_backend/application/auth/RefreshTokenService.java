package org.forif_backend.application.auth;

import lombok.RequiredArgsConstructor;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.auth.RefreshTokenStore;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.springframework.stereotype.Service;

/**
 * Refresh Token 관리 및 로테이션 서비스
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenStore refreshTokenStore;
    private final JwtProvider jwtProvider;
    private final StaffAccountRepository staffAccountRepository;

    // Refresh Token 만료 시간 (30일)
    private static final long REFRESH_TOKEN_EXPIRATION_SECONDS = 30 * 24 * 60 * 60;
    private static final String ROLE_USER = "USER";

    /**
     * Refresh Token 저장
     * @param userId 사용자 ID
     * @param refreshToken Refresh Token
     */
    public void saveRefreshToken(String userId, String role, String refreshToken) {
        refreshTokenStore.save(userId, role, refreshToken, REFRESH_TOKEN_EXPIRATION_SECONDS);
    }

    /**
     * Refresh Token 로테이션 (기존 토큰 무효화 + 새 토큰 발급)
     * refresh token에 고정된 세션 권한으로 새 토큰을 발급합니다.
     * @param oldRefreshToken 기존 Refresh Token
     * @return 새로 발급된 Access Token과 Refresh Token
     */
    public TokenPair rotateRefreshToken(String oldRefreshToken) {
        // 1. 기존 Refresh Token 검증
        if (!jwtProvider.validateToken(oldRefreshToken)) {
            throw new ForifException(ErrorCode.INVALID_TOKEN);
        }

        // 2. Refresh Token인지 확인
        if (!jwtProvider.isRefreshToken(oldRefreshToken)) {
            throw new ForifException(ErrorCode.INVALID_TOKEN);
        }

        // 3. Redis에 저장된 토큰인지 확인
        if (!refreshTokenStore.exists(oldRefreshToken)) {
            throw new ForifException(ErrorCode.INVALID_TOKEN);
        }

        // 4. 토큰에서 사용자 ID 추출
        String userId = jwtProvider.getUserIdFromToken(oldRefreshToken);

        // 5. refresh token에 고정된 세션 권한 조회
        String role = jwtProvider.getRoleFromToken(oldRefreshToken);
        validateSessionRole(userId, role);

        // 6. 기존 Refresh Token 삭제 (로테이션)
        refreshTokenStore.delete(userId, role);

        // 7. 새로운 토큰 발급
        String newAccessToken = jwtProvider.generateAccessToken(userId, role);
        String newRefreshToken = jwtProvider.generateRefreshToken(userId, role);

        // 8. 새 Refresh Token 저장
        saveRefreshToken(userId, role, newRefreshToken);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    /**
     * Refresh Token 삭제 (로그아웃 시 사용)
     * @param userId 사용자 ID
     */
    public void deleteRefreshToken(String userId, String role) {
        refreshTokenStore.delete(userId, role);
    }

    private void validateSessionRole(String userId, String role) {
        if (role == null || role.isBlank()) {
            throw new ForifException(ErrorCode.INVALID_TOKEN);
        }

        if (ROLE_USER.equals(role)) {
            return;
        }

        Long parsedUserId;
        try {
            parsedUserId = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new ForifException(ErrorCode.INVALID_TOKEN);
        }

        StaffRole staffRole;
        try {
            staffRole = StaffRole.fromValue(role);
        } catch (ForifException e) {
            throw new ForifException(ErrorCode.INVALID_TOKEN);
        }

        StaffRole currentRole = staffAccountRepository.findByUserId(parsedUserId)
                .orElseThrow(() -> new ForifException(ErrorCode.INVALID_TOKEN))
                .getRole();

        if (currentRole != staffRole) {
            throw new ForifException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * 토큰 쌍 (Access Token + Refresh Token)
     */
    public record TokenPair(
            String accessToken,
            String refreshToken
    ) {}
}
