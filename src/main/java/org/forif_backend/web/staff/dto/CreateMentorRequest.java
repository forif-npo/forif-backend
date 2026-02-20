package org.forif_backend.web.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMentorRequest(
    @NotNull Long userId,
    @NotBlank String password,
    @NotBlank String affiliation
) {
}
