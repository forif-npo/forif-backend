package org.forif_backend.application.department.dto;

import org.forif_backend.domain.department.Department;

public record DepartmentInfo(
        Long departmentId,
        String department,
        Long collegeId,
        String college
) {
    public static DepartmentInfo from(Department department) {
        return new DepartmentInfo(
                department.getId(),
                department.getDepartmentName(),
                department.getCollege().getId(),
                department.getCollege().getCollegeName()
        );
    }
}
