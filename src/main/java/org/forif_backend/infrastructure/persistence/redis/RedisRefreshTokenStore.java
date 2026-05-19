package org.forif_backend.infrastructure.persistence.redis;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.auth.RefreshTokenStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * Redis 기반 Refresh Token 저장소 구현
 */
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:session:";
    private static final String TOKEN_TO_USER_PREFIX = "refresh_token:token:";
    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String userId, String role, String refreshToken, long expirationSeconds) {
        String previousRefreshToken = get(userId, role);
        if (previousRefreshToken != null) {
            redisTemplate.delete(TOKEN_TO_USER_PREFIX + previousRefreshToken);
        }

        // 1. role + userId -> refreshToken 매핑 저장
        String sessionKey = sessionKey(userId, role);
        redisTemplate.opsForValue().set(sessionKey, refreshToken, Duration.ofSeconds(expirationSeconds));

        // 2. refreshToken -> userId 역방향 매핑 저장 (exists, getUserIdByToken 용)
        String tokenKey = TOKEN_TO_USER_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(tokenKey, userId, Duration.ofSeconds(expirationSeconds));
    }

    @Override
    public String get(String userId, String role) {
        return redisTemplate.opsForValue().get(sessionKey(userId, role));
    }

    @Override
    public void delete(String userId, String role) {
        // 1. role + userId로 기존 토큰 조회
        String refreshToken = get(userId, role);

        // 2. role + userId -> refreshToken 매핑 삭제
        redisTemplate.delete(sessionKey(userId, role));

        // 3. refreshToken -> userId 역방향 매핑 삭제
        if (refreshToken != null) {
            String tokenKey = TOKEN_TO_USER_PREFIX + refreshToken;
            redisTemplate.delete(tokenKey);
        }
    }

    @Override
    public boolean exists(String refreshToken) {
        String key = TOKEN_TO_USER_PREFIX + refreshToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public String getUserIdByToken(String refreshToken) {
        String key = TOKEN_TO_USER_PREFIX + refreshToken;
        return redisTemplate.opsForValue().get(key);
    }

    private String sessionKey(String userId, String role) {
        return REFRESH_TOKEN_PREFIX + role + ":" + userId;
    }
}
