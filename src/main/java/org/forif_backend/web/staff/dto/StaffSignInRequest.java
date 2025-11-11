package org.forif_backend.web.staff.dto;

public record StaffSignInRequest(
    Long userId,   // User ID
    String password   // 비밀번호
) {
}
