package org.forif_backend.web.user.dto;

public record MentorSignInRequest(
    String loginId,   // 멘토 로그인 ID
    String password   // 비밀번호
) {
}