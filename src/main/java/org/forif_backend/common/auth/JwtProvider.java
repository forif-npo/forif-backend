package org.forif_backend.common.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증을 담당하는 유틸리티 클래스
 * Access Token과 Refresh Token의 생성, 검증, 정보 추출 기능을 제공합니다.
 */
@Slf4j
@Component
public class JwtProvider {
    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 3600_000; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 30 * 24 * 60 * 60 * 1000L; // 30 days
    private final SecretKey key;

    public JwtProvider(@Value("${jwt.secret}") String key) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(key));
    }

    // 액세스 토큰 발급
    public String generateAccessToken(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    // 리프레시 토큰 발급
    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    // 토큰 유효성 검증 (서명 및 만료 여부 체크 포함)
    public boolean validateToken(String token) {
        try {
            log.info("Validating token: '{}'", token);
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("토큰 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    // 토큰에서 사용자 ID 가져오기
    public String getUserIdFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 토큰에서 role 가져오기
    public String getRoleFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    // 토큰 만료 여부 확인
    public boolean isExpired(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .before(new Date());
    }

    // 토큰 만료 시간 가져오기
    public Date getExpirationDate(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }
}
