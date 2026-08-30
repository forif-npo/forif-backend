package org.forif_backend.web.staff.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record UpdateMemberInfoRequest(
        @NotBlank @Length(max = 50) String department,
        @NotBlank @Length(max = 20) String phoneNum
) {
}
