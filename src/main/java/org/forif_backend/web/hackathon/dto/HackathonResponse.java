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
        String location,
        HackathonStatus status,
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
                event.getLocation(),
                event.getStatus(),
                event.getStartsAt(),
                event.getEndsAt()
        );
    }
}
