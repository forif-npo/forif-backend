package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.HackathonJoinRequest;
import org.forif_backend.domain.hackathon.JoinRequestStatus;

import java.time.LocalDateTime;

public record JoinRequestResponse(
        Long joinRequestId,
        Long hackathonId,
        Long teamId,
        Long userId,
        String userName,
        JoinRequestStatus status,
        String message,
        Long reviewedBy,
        LocalDateTime reviewedAt
) {
    public static JoinRequestResponse from(HackathonJoinRequest request) {
        return new JoinRequestResponse(
                request.getId(),
                request.getHackathon().getId(),
                request.getTeam().getId(),
                request.getUser().getId(),
                request.getUser().getUserName(),
                request.getStatus(),
                request.getMessage(),
                request.getReviewedBy() != null ? request.getReviewedBy().getId() : null,
                request.getReviewedAt()
        );
    }
}
