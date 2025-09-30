package org.forif_backend.web.user.dto;

import lombok.Builder;

@Builder
public record UserSignUpResponse(
    Long userId,
    String userName,
    String email
) {
}
