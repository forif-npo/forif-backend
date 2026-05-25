package org.forif_backend.web.hackathon.dto;

public record UpdateTeamRequest(
        String name,
        String topic,
        String description,
        Integer maxMembers
) {
}
