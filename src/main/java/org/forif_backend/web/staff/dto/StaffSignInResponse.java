package org.forif_backend.web.staff.dto;

import lombok.Builder;

@Builder
public record StaffSignInResponse(
    String accessToken,
    String refreshToken,
    String staffRole,  // "MENTOR" or "ADMIN"
    Long userId,
    String staffName
) {
}
