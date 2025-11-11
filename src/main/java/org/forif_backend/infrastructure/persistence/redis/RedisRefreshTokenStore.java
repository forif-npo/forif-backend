package org.forif_backend.infrastructure.persistence.redis;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.auth.RefreshTokenStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Set;

/**
 * Redis 기반 Refresh Token 저장소 구현
 */
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:user:";
    private static final String TOKEN_TO_USER_PREFIX = "refresh_token:token:";
    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String userId, String refreshToken, long expirationSeconds) {
        // 1. userId -> refreshToken 매핑 저장
        String userKey = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(userKey, refreshToken, Duration.ofSeconds(expirationSeconds));

        // 2. refreshToken -> userId 역방향 매핑 저장 (exists, getUserIdByToken 용)
        String tokenKey = TOKEN_TO_USER_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(tokenKey, userId, Duration.ofSeconds(expirationSeconds));
    }

    @Override
    public String get(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String userId) {
        // 1. userId로 기존 토큰 조회
        String refreshToken = get(userId);

        // 2. userId -> refreshToken 매핑 삭제
        String userKey = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(userKey);

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
}
