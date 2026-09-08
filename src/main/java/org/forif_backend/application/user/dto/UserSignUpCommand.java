package org.forif_backend.application.user.dto;

/**
 * 부원 회원가입 Command
 * Application 계층 DTO
 */
public record UserSignUpCommand(
    Long studentId,
    String userName,
    String email,
    String phoneNum,
    Long departmentId,
    String legacyDepartmentName
) {
    public UserSignUpCommand(Long studentId, String userName, String email, String phoneNum, Long departmentId) {
        this(studentId, userName, email, phoneNum, departmentId, null);
    }
}
