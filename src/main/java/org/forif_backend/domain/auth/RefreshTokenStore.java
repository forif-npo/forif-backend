package org.forif_backend.domain.auth;

/**
 * Refresh Token 저장소 도메인 포트
 * Refresh Token의 생명주기 관리
 */
public interface RefreshTokenStore {

    /**
     * Refresh Token 저장
     * @param userId 사용자 ID
     * @param refreshToken Refresh Token
     * @param expirationSeconds 토큰 만료까지 남은 시간(초)
     */
    void save(String userId, String refreshToken, long expirationSeconds);

    /**
     * 사용자 ID로 Refresh Token 조회
     * @param userId 사용자 ID
     * @return Refresh Token (없으면 null)
     */
    String get(String userId);

    /**
     * Refresh Token 삭제
     * @param userId 사용자 ID
     */
    void delete(String userId);

    /**
     * Refresh Token 존재 여부 확인
     * @param refreshToken Refresh Token
     * @return 존재하면 true
     */
    boolean exists(String refreshToken);

    /**
     * Refresh Token으로 사용자 ID 조회
     * @param refreshToken Refresh Token
     * @return 사용자 ID (없으면 null)
     */
    String getUserIdByToken(String refreshToken);
}
