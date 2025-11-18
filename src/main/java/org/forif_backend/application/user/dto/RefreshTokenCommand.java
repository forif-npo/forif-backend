package org.forif_backend.application.user.dto;

/**
 * Refresh Token Command
 * Application 계층 DTO
 */
public record RefreshTokenCommand(
    String refreshToken
) {
}
