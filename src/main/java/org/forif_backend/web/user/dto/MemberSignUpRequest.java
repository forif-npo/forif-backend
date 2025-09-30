package org.forif_backend.web.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberSignUpRequest {
    private Long studentId;     // 학번 
    private String userName;    // 이름 
    private String phoneNum;    // 전화번호 
    private String department;  // 학과 
    // email은 Google OAuth에서 가져옴
}
