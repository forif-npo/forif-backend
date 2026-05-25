package org.forif_backend.web.hackathon.dto;

public record HackathonDashboardResponse(
        long participantCount,
        long teamCount,
        long participantsWithoutTeamCount,
        long submittedTeamCount,
        long notSubmittedTeamCount,
        long evaluationCount
) {
}
