package org.forif_backend.infrastructure.persistence.redis;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.auth.TokenBlacklist;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * Redis 기반 토큰 블랙리스트 구현
 */
@Repository
@RequiredArgsConstructor
public class RedisTokenBlacklist implements TokenBlacklist {

    private static final String BLACKLIST_PREFIX = "blacklist:token:";
    private final StringRedisTemplate redisTemplate;

    @Override
    public void addToBlacklist(String token, long expirationSeconds) {
        String key = BLACKLIST_PREFIX + token;
        // value는 "blacklisted"로 저장, TTL은 토큰 만료 시간과 동일
        redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(expirationSeconds));
    }

    @Override
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
