package org.forif_backend.web.user.dto;

public record UserSignUpRequest(
    Long studentId,     // 학번
    String userName,    // 이름
    String accessToken, // Google OAuth Access Token
    String phoneNum,    // 전화번호
    String department   // 학과
) {
}
