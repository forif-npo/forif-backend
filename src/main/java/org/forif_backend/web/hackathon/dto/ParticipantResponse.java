package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.HackathonParticipant;
import org.forif_backend.domain.hackathon.ParticipantStatus;

import java.time.LocalDateTime;

public record ParticipantResponse(
        Long participantId,
        Long hackathonId,
        Long userId,
        String userName,
        ParticipantStatus status,
        LocalDateTime registeredAt,
        LocalDateTime canceledAt
) {
    public static ParticipantResponse from(HackathonParticipant participant) {
        return new ParticipantResponse(
                participant.getId(),
                participant.getHackathon().getId(),
                participant.getUser().getId(),
                participant.getUser().getUserName(),
                participant.getStatus(),
                participant.getRegisteredAt(),
                participant.getCanceledAt()
        );
    }
}
