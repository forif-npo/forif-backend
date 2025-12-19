package org.forif_backend.web.staff.dto;

import lombok.Builder;

@Builder
public record StaffSignInResponse(
    String accessToken,
    String role  // "MENTOR" or "ADMIN"
) {
}
