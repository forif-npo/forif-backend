package org.forif_backend.web.staff.dto;

public record StaffSignInRequest(
        Long userId,
        String password
) {
}
