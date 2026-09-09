package org.forif_backend.web.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Length;

public record UserSignUpRequest(
    Long studentId,     // 학번
    String userName,    // 이름
    String accessToken, // Google OAuth Access Token
    String phoneNum,    // 전화번호
    @Positive Long departmentId,   // 학과 ID
    @Length(max = 50) String department // 구 프론트 호환용 학과명
) {
    public UserSignUpRequest(Long studentId, String userName, String accessToken, String phoneNum, Long departmentId) {
        this(studentId, userName, accessToken, phoneNum, departmentId, null);
    }

    public UserSignUpRequest(Long studentId, String userName, String accessToken, String phoneNum, String department) {
        this(studentId, userName, accessToken, phoneNum, null, department);
    }

    @AssertTrue(message = "학과를 선택해주세요.")
    public boolean hasDepartmentSelection() {
        return departmentId != null || (department != null && !department.isBlank());
    }
}
