package org.forif_backend.application.staff.dto;

import org.forif_backend.domain.staff.StaffRole;

public record StaffSignUpResult(
        String accessToken,
        String refreshToken,
        StaffRole role
) {
}
