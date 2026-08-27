package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.HackathonEvent;
import org.forif_backend.domain.hackathon.HackathonStatus;

import java.time.LocalDateTime;

public record HackathonResponse(
        Long hackathonId,
        int heldYear,
        int heldSemester,
        int eventRound,
        String title,
        String description,
        String location,
        HackathonStatus status,
        LocalDateTime recruitStartsAt,
        LocalDateTime recruitEndsAt,
        LocalDateTime teamBuildingStartsAt,
        LocalDateTime teamBuildingEndsAt,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
    public static HackathonResponse from(HackathonEvent event) {
        return new HackathonResponse(
                event.getId(),
                event.getHeldYear(),
                event.getHeldSemester(),
                event.getEventRound(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStatus(),
                event.getRecruitStartsAt(),
                event.getRecruitEndsAt(),
                event.getTeamBuildingStartsAt(),
                event.getTeamBuildingEndsAt(),
                event.getStartsAt(),
                event.getEndsAt()
        );
    }
}
