package org.forif_backend.common.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.forif_backend.application.auth.TokenBlacklistService;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 삭제된 스태프 계정의 access token 을 막는 검사(FOR-123)의 경계 동작.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterStaffCheckTest {

    private static final String TOKEN = "test-token";
    private static final Long USER_ID = 20240001L;

    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private StaffAccountRepository staffAccountRepository;
    @InjectMocks
    private JwtAuthenticationFilter filter;

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void givenAdminToken() {
        when(request.getServletPath()).thenReturn("/api/v1/admin/users");
        when(request.getRequestURI()).thenReturn("/api/v1/admin/users");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
        when(jwtProvider.isTokenExpired(TOKEN)).thenReturn(false);
        when(jwtProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtProvider.isAccessToken(TOKEN)).thenReturn(true);
        when(tokenBlacklistService.isTokenBlacklisted(TOKEN)).thenReturn(false);
        when(jwtProvider.getRoleFromToken(TOKEN)).thenReturn("ADMIN");
        when(jwtProvider.getUserIdFromToken(TOKEN)).thenReturn(USER_ID.toString());
    }

    @Test
    void 삭제된_운영진_토큰은_인증되지_않는다() throws Exception {
        givenAdminToken();
        when(staffAccountRepository.existsByUserIdAndRole(USER_ID, StaffRole.ADMIN)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(request).setAttribute("jwt.error", org.forif_backend.common.exception.ErrorCode.INVALID_TOKEN);
    }

    /**
     * DB 장애를 "계정 없음"으로 뭉개면 순간적인 커넥션 문제가 운영진 전원 강제 로그아웃으로 번진다.
     * 401이 아니라 예외로 전파되어야 한다.
     */
    @Test
    void DB_장애는_삼키지_않고_전파한다() {
        givenAdminToken();
        when(staffAccountRepository.existsByUserIdAndRole(USER_ID, StaffRole.ADMIN))
                .thenThrow(new QueryTimeoutException("connection pool exhausted"));

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(jakarta.servlet.ServletException.class)
                .hasCauseInstanceOf(QueryTimeoutException.class);
    }

    /**
     * 기존 MENTOR 토큰은 별도 역할을 얻지 않는다. 멘토 접근은 일반 USER 토큰과 같이
     * 각 기능에서 tb_study의 멘토 관계로 검사한다.
     */
    @Test
    void 기존_멘토_토큰은_일반_부원_권한으로만_인증된다() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/studies/1/attendance");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
        when(jwtProvider.isTokenExpired(TOKEN)).thenReturn(false);
        when(jwtProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtProvider.isAccessToken(TOKEN)).thenReturn(true);
        when(tokenBlacklistService.isTokenBlacklisted(TOKEN)).thenReturn(false);
        when(jwtProvider.getRoleFromToken(TOKEN)).thenReturn("MENTOR");
        when(jwtProvider.getUserIdFromToken(TOKEN)).thenReturn(USER_ID.toString());

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_USER");
    }

    @Test
    void 일반_부원_토큰은_스태프_계정을_조회하지_않는다() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/users/me");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
        when(jwtProvider.isTokenExpired(TOKEN)).thenReturn(false);
        when(jwtProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtProvider.isAccessToken(TOKEN)).thenReturn(true);
        when(tokenBlacklistService.isTokenBlacklisted(TOKEN)).thenReturn(false);
        when(jwtProvider.getRoleFromToken(TOKEN)).thenReturn("USER");
        when(jwtProvider.getUserIdFromToken(TOKEN)).thenReturn(USER_ID.toString());

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
