package org.forif_backend.web.user.dto;

import lombok.Builder;

@Builder
public record UserResponse(
        Long id,
        String email
) {
}
