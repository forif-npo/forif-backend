package org.forif_backend.web.hackathon.dto;

import java.time.LocalDateTime;

public record UpdateHackathonRequest(
        String title,
        String description,
        String location,
        LocalDateTime recruitStartsAt,
        LocalDateTime recruitEndsAt,
        LocalDateTime teamBuildingStartsAt,
        LocalDateTime teamBuildingEndsAt,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
}
