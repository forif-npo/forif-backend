package org.forif_backend.application.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.auth.RefreshTokenStore;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

    private JwtProvider jwtProvider;
    private InMemoryRefreshTokenStore refreshTokenStore;
    private StaffAccountRepository staffAccountRepository;
    private RefreshTokenService refreshTokenService;
    private String jwtSecret;

    @BeforeEach
    void setUp() {
        jwtSecret = Base64.getEncoder()
                .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));
        jwtProvider = new JwtProvider(jwtSecret);
        refreshTokenStore = new InMemoryRefreshTokenStore();
        staffAccountRepository = mock(StaffAccountRepository.class);
        refreshTokenService = new RefreshTokenService(refreshTokenStore, jwtProvider, staffAccountRepository);
    }

    @Test
    @DisplayName("USER 세션 refresh는 같은 userId의 staff 계정이 있어도 USER로만 재발급된다")
    void userRefreshTokenDoesNotPromoteToStaffRole() {
        String refreshToken = jwtProvider.generateRefreshToken("1", "USER");
        refreshTokenService.saveRefreshToken("1", "USER", refreshToken);

        RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotateRefreshToken(refreshToken);

        assertThat(jwtProvider.getRoleFromToken(tokenPair.accessToken())).isEqualTo("USER");
        assertThat(jwtProvider.isAccessToken(tokenPair.accessToken())).isTrue();
        assertThat(jwtProvider.isRefreshToken(tokenPair.refreshToken())).isTrue();
        verifyNoInteractions(staffAccountRepository);
    }

    @Test
    @DisplayName("MENTOR 세션 refresh는 해당 유저의 MENTOR 계정이 존재할 때만 MENTOR로 재발급된다")
    void mentorRefreshTokenKeepsMentorRole() {
        String refreshToken = jwtProvider.generateRefreshToken("1", "MENTOR");
        refreshTokenService.saveRefreshToken("1", "MENTOR", refreshToken);
        when(staffAccountRepository.existsByUserIdAndRole(1L, StaffRole.MENTOR)).thenReturn(true);

        RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotateRefreshToken(refreshToken);

        assertThat(jwtProvider.getRoleFromToken(tokenPair.accessToken())).isEqualTo("MENTOR");
    }

    @Test
    @DisplayName("MENTOR 계정이 삭제된 세션은 refresh되지 않는다 (ADMIN 계정만 남아 있어도 실패)")
    void mentorRefreshTokenFailsWhenMentorAccountRemoved() {
        String refreshToken = jwtProvider.generateRefreshToken("1", "MENTOR");
        refreshTokenService.saveRefreshToken("1", "MENTOR", refreshToken);
        when(staffAccountRepository.existsByUserIdAndRole(1L, StaffRole.MENTOR)).thenReturn(false);

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(refreshToken))
                .isInstanceOfSatisfying(ForifException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("Redis에 없는 refresh token은 재사용 공격으로 보고 실패한다")
    void refreshTokenMissingFromStoreCannotBeRotated() {
        String refreshToken = jwtProvider.generateRefreshToken("1", "USER");

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(refreshToken))
                .isInstanceOfSatisfying(ForifException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("Access Token은 refresh에 사용할 수 없다")
    void accessTokenCannotBeUsedAsRefreshToken() {
        String accessToken = jwtProvider.generateAccessToken("1", "ADMIN");
        refreshTokenService.saveRefreshToken("1", "ADMIN", accessToken);

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(accessToken))
                .isInstanceOfSatisfying(ForifException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("tokenType이 없는 role 포함 토큰은 Access Token으로 인정하지 않는다")
    void tokenWithoutTypeIsNotAcceptedAsAccessToken() {
        String legacyRoleToken = Jwts.builder()
                .setSubject("1")
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                .compact();

        assertThat(jwtProvider.isAccessToken(legacyRoleToken)).isFalse();
    }

    private static class InMemoryRefreshTokenStore implements RefreshTokenStore {

        private final Map<String, String> sessionTokens = new HashMap<>();
        private final Map<String, String> tokenUsers = new HashMap<>();

        @Override
        public void save(String userId, String role, String refreshToken, long expirationSeconds) {
            String sessionKey = sessionKey(userId, role);
            String previousRefreshToken = sessionTokens.put(sessionKey, refreshToken);
            if (previousRefreshToken != null) {
                tokenUsers.remove(previousRefreshToken);
            }
            tokenUsers.put(refreshToken, userId);
        }

        @Override
        public String get(String userId, String role) {
            return sessionTokens.get(sessionKey(userId, role));
        }

        @Override
        public void delete(String userId, String role) {
            String refreshToken = sessionTokens.remove(sessionKey(userId, role));
            if (refreshToken != null) {
                tokenUsers.remove(refreshToken);
            }
        }

        @Override
        public boolean exists(String refreshToken) {
            return tokenUsers.containsKey(refreshToken);
        }

        @Override
        public String getUserIdByToken(String refreshToken) {
            return tokenUsers.get(refreshToken);
        }

        private String sessionKey(String userId, String role) {
            return role + ":" + userId;
        }
    }
}
