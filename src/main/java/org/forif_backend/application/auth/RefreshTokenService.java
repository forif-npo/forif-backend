package org.forif_backend.application.auth;

import lombok.RequiredArgsConstructor;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.auth.RefreshTokenStore;
import org.forif_backend.domain.staff.StaffAccountRepository;
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

    /**
     * Refresh Token 저장
     * @param userId 사용자 ID
     * @param refreshToken Refresh Token
     */
    public void saveRefreshToken(String userId, String refreshToken) {
        refreshTokenStore.save(userId, refreshToken, REFRESH_TOKEN_EXPIRATION_SECONDS);
    }

    /**
     * Refresh Token 로테이션 (기존 토큰 무효화 + 새 토큰 발급)
     * DB에서 현재 역할을 조회하여 Access Token에 반영합니다.
     * @param oldRefreshToken 기존 Refresh Token
     * @return 새로 발급된 Access Token과 Refresh Token
     */
    public TokenPair rotateRefreshToken(String oldRefreshToken) {
        // 1. 기존 Refresh Token 검증
        if (!jwtProvider.validateToken(oldRefreshToken)) {
            throw new ForifException(ErrorCode.INVALID_TOKEN);
        }

        // 2. Redis에 저장된 토큰인지 확인
        if (!refreshTokenStore.exists(oldRefreshToken)) {
            throw new ForifException(ErrorCode.INVALID_TOKEN);
        }

        // 3. 토큰에서 사용자 ID 추출
        String userId = jwtProvider.getUserIdFromToken(oldRefreshToken);

        // 4. DB에서 현재 역할 조회
        String role = staffAccountRepository.findByUserId(Long.parseLong(userId))
                .map(staff -> staff.getRole().getValue())
                .orElse("USER");

        // 5. 기존 Refresh Token 삭제 (로테이션)
        refreshTokenStore.delete(userId);

        // 6. 새로운 토큰 발급
        String newAccessToken = jwtProvider.generateAccessToken(userId, role);
        String newRefreshToken = jwtProvider.generateRefreshToken(userId);

        // 7. 새 Refresh Token 저장
        saveRefreshToken(userId, newRefreshToken);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    /**
     * Refresh Token 삭제 (로그아웃 시 사용)
     * @param userId 사용자 ID
     */
    public void deleteRefreshToken(String userId) {
        refreshTokenStore.delete(userId);
    }

    /**
     * 토큰 쌍 (Access Token + Refresh Token)
     */
    public record TokenPair(
            String accessToken,
            String refreshToken
    ) {}
}
