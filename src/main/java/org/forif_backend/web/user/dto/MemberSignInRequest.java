package org.forif_backend.web.user.dto;

public record MemberSignInRequest(
    String accessToken  // Google OAuth Access Token
) {
}