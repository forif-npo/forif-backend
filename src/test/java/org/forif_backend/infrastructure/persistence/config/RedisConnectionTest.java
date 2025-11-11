package org.forif_backend.infrastructure.persistence.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Redis infrastructure test - not needed in CI/CD")
@SpringBootTest
@ActiveProfiles("local")
class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void redisConnectionTest() {
        // given
        String key = "testKey";
        String value = "testValue";

        // when
        stringRedisTemplate.opsForValue().set(key, value);
        String result = stringRedisTemplate.opsForValue().get(key);

        // then
        assertThat(result).isEqualTo(value);

        // cleanup
        stringRedisTemplate.delete(key);
    }

    @Test
    void redisTemplateConnectionTest() {
        // given
        String key = "testObjectKey";
        String value = "testObjectValue";

        // when
        redisTemplate.opsForValue().set(key, value);
        Object result = redisTemplate.opsForValue().get(key);

        // then
        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("testObjectValue");

        // cleanup
        redisTemplate.delete(key);
    }

    @Test
    void redisPingTest() {
        // Redis 서버에 ping 명령 전송
        String pong = stringRedisTemplate.getConnectionFactory()
                .getConnection()
                .ping();

        // "PONG" 응답 확인
        assertThat(pong).isEqualTo("PONG");
    }
}
