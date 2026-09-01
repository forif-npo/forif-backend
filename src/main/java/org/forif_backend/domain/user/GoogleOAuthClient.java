package org.forif_backend.domain.user;

public interface GoogleOAuthClient {

    // Google OAuth Access Token으로 사용자 이메일 조회
    String getEmailFromToken(String token);
}
