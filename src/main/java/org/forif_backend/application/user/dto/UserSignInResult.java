package org.forif_backend.application.user.dto;

/**
 * 부원 로그인 Result
 * Application 계층 DTO
 */
public record UserSignInResult(
    String accessToken,
    String refreshToken,
    String role
) {
}
