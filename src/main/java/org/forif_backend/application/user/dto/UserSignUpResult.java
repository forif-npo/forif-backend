package org.forif_backend.application.user.dto;

/**
 * 부원 회원가입 Result
 * Application 계층 DTO
 */
public record UserSignUpResult(
    Long userId,
    String userName,
    String email
) {
}
