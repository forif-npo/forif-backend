package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.CompetitionType;

public record UpdateTeamRequest(
        String name,
        String topic,
        String description,
        CompetitionType competitionType,
        Integer maxMembers
) {
}
