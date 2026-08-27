package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.HackathonEvent;
import org.forif_backend.domain.hackathon.HackathonStatus;
import org.forif_backend.domain.hackathon.CompetitionType;

import java.time.LocalDateTime;
import java.util.List;

public record ArchiveHackathonDetailResponse(
        Long hackathonId,
        int heldYear,
        int heldSemester,
        int eventRound,
        CompetitionType competitionType,
        String title,
        String description,
        String location,
        HackathonStatus status,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        long participantCount,
        long teamCount,
        long submissionCount,
        List<AwardResponse> awards
) {
    public static ArchiveHackathonDetailResponse of(HackathonEvent event,
                                                    long participantCount,
                                                    long teamCount,
                                                    long submissionCount,
                                                    List<AwardResponse> awards) {
        return new ArchiveHackathonDetailResponse(
                event.getId(),
                event.getHeldYear(),
                event.getHeldSemester(),
                event.getEventRound(),
                event.getCompetitionType(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStatus(),
                event.getStartsAt(),
                event.getEndsAt(),
                participantCount,
                teamCount,
                submissionCount,
                awards
        );
    }
}
