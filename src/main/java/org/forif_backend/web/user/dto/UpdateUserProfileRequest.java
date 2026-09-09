package org.forif_backend.web.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Length;

public record UpdateUserProfileRequest(
        @Positive Long departmentId,
        @Length(max = 50) String department
) {
    @AssertTrue(message = "학과를 선택해주세요.")
    public boolean hasDepartmentSelection() {
        return departmentId != null || (department != null && !department.isBlank());
    }
}
