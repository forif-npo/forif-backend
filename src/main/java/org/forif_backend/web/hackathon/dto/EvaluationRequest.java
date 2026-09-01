package org.forif_backend.web.hackathon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EvaluationRequest(
        @NotEmpty List<@Valid Score> scores
) {
    public record Score(
            @NotNull Long criterionId,
            @NotNull Integer score
    ) {
    }
}
