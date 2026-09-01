package org.forif_backend.application.auth;

import lombok.RequiredArgsConstructor;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.domain.auth.TokenBlacklist;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 토큰 블랙리스트 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final TokenBlacklist tokenBlacklist;
    private final JwtProvider jwtProvider;

    /**
     * Access Token을 블랙리스트에 추가
     * @param token 블랙리스트에 추가할 Access Token
     */
    public void blacklistToken(String token) {
        // 토큰 만료 시간 계산
        long expirationSeconds = getTokenExpirationSeconds(token);

        if (expirationSeconds > 0) {
            tokenBlacklist.addToBlacklist(token, expirationSeconds);
        }
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     * @param token 확인할 토큰
     * @return 블랙리스트에 있으면 true
     */
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.isBlacklisted(token);
    }

    /**
     * 토큰의 남은 만료 시간(초) 계산
     */
    private long getTokenExpirationSeconds(String token) {
        try {
            Date expiration = jwtProvider.getExpirationDate(token);
            long now = System.currentTimeMillis();
            long expirationTime = expiration.getTime();

            return (expirationTime - now) / 1000;
        } catch (Exception e) {
            return 0;
        }
    }
}
