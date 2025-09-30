package org.forif_backend.web.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberSignInRequest {
    private String accessToken;  // Google OAuth Access Token
}