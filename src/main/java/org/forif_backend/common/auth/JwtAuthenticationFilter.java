package org.forif_backend.common.auth;


import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;


import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.FilterChain;
import java.io.IOException;
import java.util.List;

import org.forif_backend.application.auth.TokenBlacklistService;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;

/**
 * JWT 토큰 기반 인증을 처리하는 Spring Security 필터
 * HTTP 요청에서 JWT 토큰을 추출하고 검증하여 사용자 인증을 수행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final StaffAccountRepository staffAccountRepository;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String[] PUBLIC_PATHS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/scalar",
            "/scalar/**",
            "/favicon.ico"
    };

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        for (String publicPath : PUBLIC_PATHS) {
            if (PATH_MATCHER.match(publicPath, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
        throws ServletException, IOException {
            
            try {
                // 1. Authorization Header에서 JWT 토큰 추출
                String token = extractTokenFromRequest(request);

                // 2. 토큰이 없으면 다음 필터로 진행 (공개 경로 요청마다 발생하는 정상 흐름이므로 debug)
                if(token == null) {
                    log.debug("토큰이 없습니다. URI: {}", request.getRequestURI());
                    request.setAttribute("jwt.error", ErrorCode.MISSING_TOKEN);
                    filterChain.doFilter(request, response);
                    return;
                }

                // 3. 토큰 만료 여부 확인 (validateToken 전에 먼저 체크)
                if(jwtProvider.isTokenExpired(token)) {
                    log.debug("토큰이 만료되었습니다. URI: {}", request.getRequestURI());
                    request.setAttribute("jwt.error", ErrorCode.TOKEN_EXPIRED);
                    filterChain.doFilter(request, response);
                    return;
                }

                // 4. 토큰이 유효한지 검증
                if(!jwtProvider.validateToken(token)) {
                    log.warn("토큰이 유효하지 않습니다. URI: {}", request.getRequestURI());
                    request.setAttribute("jwt.error", ErrorCode.INVALID_TOKEN);
                    filterChain.doFilter(request, response);
                    return;
                }

                // 5. 인증 헤더에는 Access Token만 허용
                if (!jwtProvider.isAccessToken(token)) {
                    log.warn("Access Token이 아닙니다. URI: {}", request.getRequestURI());
                    request.setAttribute("jwt.error", ErrorCode.INVALID_TOKEN);
                    filterChain.doFilter(request, response);
                    return;
                }

                // 6. 블랙리스트 확인 (로그아웃된 토큰인지 검증)
                if(tokenBlacklistService.isTokenBlacklisted(token)) {
                    log.warn("블랙리스트에 등록된 토큰입니다. URI: {}", request.getRequestURI());
                    request.setAttribute("jwt.error", ErrorCode.INVALID_TOKEN);
                    filterChain.doFilter(request, response);
                    return;
                }

                // 운영진/멘토 계정이 삭제된 뒤에는 만료 전 access token도 더 이상 권한을 갖지 않는다.
                if (!isActiveStaffAccount(token)) {
                    log.warn("삭제된 스태프 계정의 토큰입니다. URI: {}", request.getRequestURI());
                    request.setAttribute("jwt.error", ErrorCode.INVALID_TOKEN);
                    filterChain.doFilter(request, response);
                    return;
                }

                // 7. 토큰에서 사용자 ID 추출 및 인증 정보 설정
                setAuthentication(token, request);
                log.debug("JWT 인증 성공. ID: {}", jwtProvider.getUserIdFromToken(token));

            } catch (DataAccessException e) {
                // 조회가 실패한 것과 권한이 없는 것은 다르다. 여기서 삼키면 순간적인 DB 장애가
                // 인증 실패로 둔갑해 운영진 전원이 로그아웃된다. 500으로 드러내는 편이 낫다.
                log.error("인증 확인 중 DB 접근 실패. URI: {}", request.getRequestURI(), e);
                throw new ServletException("인증 확인 중 데이터 접근에 실패했습니다", e);
            } catch (Exception e) {
                log.error("JWT 인증 실패: {}", e.getMessage(), e);
            }

            filterChain.doFilter(request, response);
        }


        private String extractTokenFromRequest(HttpServletRequest request) {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

            if(StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
                return authorization.substring(BEARER_PREFIX_LENGTH);
            }
            return null;
        }

        private void setAuthentication(String token, HttpServletRequest request) {

            String userIdStr = jwtProvider.getUserIdFromToken(token);
            Long userId = Long.parseLong(userIdStr);
            String role = jwtProvider.getRoleFromToken(token);

            List<SimpleGrantedAuthority> authorities = resolveAuthorities(role);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities
                    );

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        /**
         * 스태프 계정이 아직 살아 있는지 확인한다.
         *
         * 멘토는 검사하지 않는다. 멘토 권한은 계정이 아니라 tb_study의 멘토 관계에서
         * 유도되므로(FOR-116), 여기서 tb_staff_account 존재를 요구하면 멘토 계정 정리 시
         * 로그인 중인 멘토가 전부 끊긴다.
         *
         * DB 예외는 삼키지 않는다. 조회가 실패한 것과 계정이 없는 것은 다르다.
         * 이를 false로 뭉개면 순간적인 DB 장애가 운영진 전원 강제 로그아웃으로 번진다.
         */
        private boolean isActiveStaffAccount(String token) {
            String role = jwtProvider.getRoleFromToken(token);
            if (!"ADMIN".equals(role)) {
                return true;
            }

            Long userId;
            try {
                userId = Long.parseLong(jwtProvider.getUserIdFromToken(token));
            } catch (NumberFormatException e) {
                return false;   // 토큰이 망가진 경우
            }
            return staffAccountRepository.existsByUserIdAndRole(userId, StaffRole.ADMIN);
        }

        private List<SimpleGrantedAuthority> resolveAuthorities(String role) {
            if ("ADMIN".equals(role)) {
                return List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_MENTOR"),
                        new SimpleGrantedAuthority("ROLE_USER")
                );
            }
            if ("MENTOR".equals(role)) {
                return List.of(
                        new SimpleGrantedAuthority("ROLE_MENTOR"),
                        new SimpleGrantedAuthority("ROLE_USER")
                );
            }
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
}
