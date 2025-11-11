package org.forif_backend.web.user.dto;

import lombok.Builder;

@Builder
public record UserSignInResponse(
    String accessToken,
    String role  // "USER"
) {
}