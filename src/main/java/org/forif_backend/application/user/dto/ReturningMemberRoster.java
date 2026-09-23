package org.forif_backend.application.user.dto;

import org.forif_backend.domain.department.Department;
import org.forif_backend.domain.user.User;

import java.util.List;

public record ReturningMemberRoster(int actYear, int actSemester, List<Member> members) {
    public record Member(String userId, String userName, String college, String department, String phoneNum) {
        public static Member from(User user) {
            Department department = user.getDepartmentEntity();
            return new Member(
                    user.getId().toString(), user.getUserName(),
                    department == null ? "" : department.getCollege().getCollegeName(),
                    department == null ? user.getDepartment() : department.getDepartmentName(),
                    user.getPhoneNum());
        }
    }
}
