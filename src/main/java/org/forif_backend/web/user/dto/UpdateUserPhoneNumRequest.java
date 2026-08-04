package org.forif_backend.web.user.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record UpdateUserPhoneNumRequest(
        @NotBlank
        @Length(max = 20)
        String phoneNum
) {
}
