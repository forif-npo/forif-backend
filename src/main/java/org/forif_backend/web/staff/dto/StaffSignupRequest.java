package org.forif_backend.web.staff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forif_backend.domain.staff.StaffRole;

public record StaffSignupRequest(
        Long userId,

        String name,

        String password,

        StaffRole role,

        String affiliation
) {
}
