package org.forif_backend.web.staff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forif_backend.domain.staff.StaffRole;

public record StaffSignupRequest(
        @JsonProperty("userId")
        Long userId,

        @JsonProperty("name")
        String name,

        @JsonProperty("password")
        String password,

        @JsonProperty("role")
        StaffRole role,

        @JsonProperty("affiliation")
        String affiliation
) {
}
