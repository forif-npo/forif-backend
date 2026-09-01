package org.forif_backend.web.user.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record UpdateUserProfileRequest(
        @NotBlank
        @Length(max = 50)
        String department
) {
}
