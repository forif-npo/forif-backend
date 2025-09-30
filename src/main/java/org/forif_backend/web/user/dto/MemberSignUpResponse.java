package org.forif_backend.web.user.dto;

import lombok.Builder;

@Builder
public record MemberSignUpResponse(
    Long userId,
    String userName,
    String email,
    String message
) {
    public static MemberSignUpResponse of(Long userId, String userName, String email) {
        return MemberSignUpResponse.builder()
            .userId(userId)
            .userName(userName)
            .email(email)
            .message("회원가입이 완료되었습니다.")
            .build();
    }
}
