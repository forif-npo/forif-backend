package org.forif_backend.web.user.dto;

public record UserSignInRequest(
    String accessToken  // Google OAuth Access Token
) {
}
