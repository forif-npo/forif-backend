package org.forif_backend.application.staff.dto;

import org.forif_backend.domain.staff.StaffRole;

/**
 * 스태프 로그인 Command
 * Application 계층 DTO
 *
 * @param role 로그인할 계정 역할. null이면 비밀번호가 일치하는 계정(ADMIN 우선)으로 로그인.
 */
public record StaffSignInCommand(
    Long userId,
    String password,
    StaffRole role
) {
}
