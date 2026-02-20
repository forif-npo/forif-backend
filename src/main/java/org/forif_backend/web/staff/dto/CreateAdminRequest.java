package org.forif_backend.web.staff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateAdminRequest(
        @JsonProperty("user_id") Long userId,
        String password,
        String affiliation
) {}
