package org.forif_backend.application.staff.dto;

/**
 * 스태프 로그인 Command
 * Application 계층 DTO
 */
public record StaffSignInCommand(
    Long userId,
    String password
) {
}
