package org.forif_backend.web.user.dto;

import lombok.Builder;

@Builder
public record SignInResponse(
    String accessToken,
    String refreshToken,
    String userType,  // "MEMBER" or "MENTOR"
    Long userId,
    String userName
) {
}