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
import static org.mockito.Mockito.never;
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
     * 멘토 권한은 계정이 아니라 tb_study의 멘토 관계에서 유도된다(FOR-116).
     * 여기서 스태프 계정을 요구하면 멘토 계정 정리 시 로그인 중인 멘토가 전부 끊긴다.
     */
    @Test
    void 멘토_토큰은_스태프_계정을_조회하지_않는다() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/studies/1/attendance");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
        when(jwtProvider.isTokenExpired(TOKEN)).thenReturn(false);
        when(jwtProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtProvider.isAccessToken(TOKEN)).thenReturn(true);
        when(tokenBlacklistService.isTokenBlacklisted(TOKEN)).thenReturn(false);
        when(jwtProvider.getRoleFromToken(TOKEN)).thenReturn("MENTOR");
        when(jwtProvider.getUserIdFromToken(TOKEN)).thenReturn(USER_ID.toString());

        filter.doFilter(request, response, chain);

        verify(staffAccountRepository, never()).existsByUserIdAndRole(USER_ID, StaffRole.MENTOR);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
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

        verify(staffAccountRepository, never()).existsByUserIdAndRole(USER_ID, StaffRole.ADMIN);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
