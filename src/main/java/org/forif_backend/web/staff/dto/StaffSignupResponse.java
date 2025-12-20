package org.forif_backend.web.staff.dto;

import lombok.Builder;
import org.forif_backend.domain.staff.StaffRole;

@Builder
public record StaffSignupResponse(
        String accessToken,
        StaffRole role
) {
}
