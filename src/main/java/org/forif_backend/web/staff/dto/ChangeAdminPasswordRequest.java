package org.forif_backend.web.staff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeAdminPasswordRequest(
        @JsonProperty("current_password") @NotBlank String currentPassword,
        @JsonProperty("new_password") @NotBlank @Size(min = 8, max = 20) String newPassword
) {}
