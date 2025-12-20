package org.forif_backend.web.staff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StaffSignInRequest(
        @JsonProperty("userId") Long userId,
        @JsonProperty("password") String password
) {
}
