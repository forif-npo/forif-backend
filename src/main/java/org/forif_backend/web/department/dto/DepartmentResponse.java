package org.forif_backend.web.department.dto;

import org.forif_backend.application.department.dto.DepartmentInfo;

public record DepartmentResponse(
        Long departmentId,
        String department,
        Long collegeId,
        String college
) {
    public static DepartmentResponse from(DepartmentInfo info) {
        return new DepartmentResponse(info.departmentId(), info.department(), info.collegeId(), info.college());
    }
}
