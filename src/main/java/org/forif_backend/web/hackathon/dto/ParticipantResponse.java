package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.HackathonParticipant;
import org.forif_backend.domain.hackathon.ParticipantStatus;
import org.forif_backend.domain.study.Study;

import java.time.LocalDateTime;
import java.util.List;

public record ParticipantResponse(
        Long participantId,
        Long hackathonId,
        Long userId,
        String userName,
        ParticipantStatus status,
        LocalDateTime registeredAt,
        LocalDateTime canceledAt,
        List<ParticipantStudyResponse> studies
) {
    public static ParticipantResponse from(HackathonParticipant participant) {
        return from(participant, List.of());
    }

    public static ParticipantResponse from(
            HackathonParticipant participant,
            List<ParticipantStudyResponse> studies
    ) {
        return new ParticipantResponse(
                participant.getId(),
                participant.getHackathon().getId(),
                participant.getUser().getId(),
                participant.getUser().getUserName(),
                participant.getStatus(),
                participant.getRegisteredAt(),
                participant.getCanceledAt(),
                studies
        );
    }

    public record ParticipantStudyResponse(
            Integer studyId,
            String studyName,
            ParticipantStudyRole role
    ) {
        public static ParticipantStudyResponse of(Study study, ParticipantStudyRole role) {
            return new ParticipantStudyResponse(
                    study.getId(),
                    study.getStudyName(),
                    role
            );
        }
    }

    public enum ParticipantStudyRole {
        MENTEE,
        MENTOR
    }
}
