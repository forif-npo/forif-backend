package org.forif_backend.application.staff.dto;

/**
 * 스태프 로그인 Result
 * Application 계층 DTO
 */
public record StaffSignInResult(
    String accessToken,
    String refreshToken,
    String role,
    String affiliation
) {
}
