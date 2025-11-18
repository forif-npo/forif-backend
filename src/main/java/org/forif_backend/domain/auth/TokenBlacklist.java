package org.forif_backend.domain.auth;

/**
 * 토큰 블랙리스트 도메인 포트
 * 로그아웃된 Access Token을 관리
 */
public interface TokenBlacklist {

    /**
     * 토큰을 블랙리스트에 추가
     * @param token 블랙리스트에 추가할 토큰
     * @param expirationSeconds 토큰 만료까지 남은 시간(초)
     */
    void addToBlacklist(String token, long expirationSeconds);

    /**
     * 토큰이 블랙리스트에 있는지 확인
     * @param token 확인할 토큰
     * @return 블랙리스트에 있으면 true
     */
    boolean isBlacklisted(String token);
}
