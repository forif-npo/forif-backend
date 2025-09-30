package org.forif_backend.web.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MentorSignInRequest {
    private String loginId;   // 멘토 로그인 ID
    private String password;  // 비밀번호
}