package org.forif_backend.application.staff.dto;

/** 운영진 스태프 로그인 Command */
public record StaffSignInCommand(
    Long userId,
    String password
) {
}
