package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.HackathonEvent;
import org.forif_backend.domain.hackathon.HackathonStatus;
import org.forif_backend.domain.hackathon.CompetitionType;

import java.time.LocalDateTime;

public record HackathonDetailResponse(
        Long hackathonId,
        int heldYear,
        int heldSemester,
        int eventRound,
        CompetitionType competitionType,
        String title,
        String description,
        String location,
        HackathonStatus status,
        LocalDateTime recruitStartsAt,
        LocalDateTime recruitEndsAt,
        LocalDateTime teamBuildingStartsAt,
        LocalDateTime teamBuildingEndsAt,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        LocalDateTime serverTime
) {
    public static HackathonDetailResponse from(HackathonEvent event, LocalDateTime serverTime) {
        return new HackathonDetailResponse(
                event.getId(),
                event.getHeldYear(),
                event.getHeldSemester(),
                event.getEventRound(),
                event.getCompetitionType(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStatus(),
                event.getRecruitStartsAt(),
                event.getRecruitEndsAt(),
                event.getTeamBuildingStartsAt(),
                event.getTeamBuildingEndsAt(),
                event.getStartsAt(),
                event.getEndsAt(),
                serverTime
        );
    }
}
