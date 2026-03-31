package org.forif_backend.web.staff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record AdminResponse(
        @JsonProperty("user_id") Long userId,
        String name,
        String department,
        @JsonProperty("phone_num") String phoneNum,
        String affiliation
) {}
