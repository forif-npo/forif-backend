package org.forif_backend.application.user.dto;

/**
 * 부원 로그인 Command
 * Application 계층 DTO
 */
public record UserSignInCommand(
    String email
) {
}
