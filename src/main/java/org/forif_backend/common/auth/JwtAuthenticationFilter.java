package org.forif_backend.common.auth;


import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.http.HttpHeaders;
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
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
        throws ServletException, IOException {
            
            try {
                // 1. Authorization Header에서 JWT 토큰 추출
                String token = extractTokenFromRequest(request);

                // 2. 토큰이 없으면 다음 필터로 진행
                if(token == null) {
                    log.info("토큰이 없습니다. URI: {}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                // 3. 토큰이 유효한지 검증
                if(!jwtProvider.validateToken(token)) {
                    log.error("토큰이 유효하지 않습니다. URI: {}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                // 4. 토큰 만료 여부 확인
                if(jwtProvider.isExpired(token)) {
                    log.info("토큰이 만료되었습니다. URI: {}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                // 5. 블랙리스트 확인 (로그아웃된 토큰인지 검증)
                if(tokenBlacklistService.isTokenBlacklisted(token)) {
                    log.warn("블랙리스트에 등록된 토큰입니다. URI: {}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                // 6. 토큰에서 사용자 ID 추출 및 인증 정보 설정
                setAuthentication(token, request);
                log.info("JWT 인증 성공. ID: {}", jwtProvider.getUserIdFromToken(token));

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
            
            String userId = jwtProvider.getUserIdFromToken(token);

            UsernamePasswordAuthenticationToken authenticationToken = 
                    new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
}
