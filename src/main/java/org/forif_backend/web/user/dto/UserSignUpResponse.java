package org.forif_backend.web.user.dto;

import lombok.Builder;

@Builder
public record UserSignUpResponse(
    String accessToken,
    String role  // "USER"
) {
}
