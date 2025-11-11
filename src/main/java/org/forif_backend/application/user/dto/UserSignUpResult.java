package org.forif_backend.application.user.dto;

/**
 * 부원 회원가입 Result
 * Application 계층 DTO
 */
public record UserSignUpResult(
    String accessToken,
    String refreshToken,
    String role
) {
}
