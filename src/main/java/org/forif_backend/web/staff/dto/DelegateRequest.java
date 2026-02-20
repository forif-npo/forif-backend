package org.forif_backend.web.staff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DelegateRequest(
        @JsonProperty("user_id") Long userId,
        String affiliation
) {}
