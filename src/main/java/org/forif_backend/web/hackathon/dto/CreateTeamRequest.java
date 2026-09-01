package org.forif_backend.web.hackathon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.forif_backend.domain.hackathon.CompetitionType;

public record CreateTeamRequest(
        @NotBlank String name,
        String topic,
        String description,
        @NotNull CompetitionType competitionType,
        Integer maxMembers
) {
}
