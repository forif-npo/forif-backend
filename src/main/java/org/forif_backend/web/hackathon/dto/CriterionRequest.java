package org.forif_backend.web.hackathon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CriterionRequest(
        @NotBlank String name,
        String description,
        Integer maxScore,
        BigDecimal weight,
        @NotNull Integer displayOrder
) {
}
