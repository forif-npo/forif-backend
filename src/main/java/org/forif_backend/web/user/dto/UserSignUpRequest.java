package org.forif_backend.web.user.dto;

public record UserSignUpRequest(
    Long studentId,     // 학번
    String userName,    // 이름
    String email,       // 이메일 (프론트에서 Google OAuth로 획득)
    String phoneNum,    // 전화번호
    String department   // 학과
) {
}
