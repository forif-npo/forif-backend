package org.forif_backend.web.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Length;

public record UpdateMemberInfoRequest(
        @Positive Long departmentId,
        @Length(max = 50) String department,
        @NotBlank @Length(max = 20) String phoneNum
) {
    @AssertTrue(message = "학과를 선택해주세요.")
    public boolean hasDepartmentSelection() {
        return departmentId != null || (department != null && !department.isBlank());
    }
}
