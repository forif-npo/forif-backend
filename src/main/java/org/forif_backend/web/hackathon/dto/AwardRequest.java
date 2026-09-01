package org.forif_backend.web.hackathon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AwardRequest(
        @NotNull Long hackathonTeamId,
        @NotBlank String awardName,
        Integer awardRank
) {
}
