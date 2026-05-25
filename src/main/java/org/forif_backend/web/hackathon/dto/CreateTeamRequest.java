package org.forif_backend.web.hackathon.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTeamRequest(
        @NotBlank String name,
        String topic,
        String description,
        Integer maxMembers
) {
}
