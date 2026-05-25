package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.HackathonSubmission;
import org.forif_backend.domain.hackathon.HackathonTeam;

import java.util.List;

public record SubmissionStatusResponse(
        Long hackathonTeamId,
        String teamName,
        Long leaderId,
        String leaderName,
        long memberCount,
        boolean submitted,
        SubmissionResponse submission
) {
    public static SubmissionStatusResponse of(HackathonTeam team,
                                              long memberCount,
                                              HackathonSubmission submission,
                                              List<String> techStacks) {
        return new SubmissionStatusResponse(
                team.getId(),
                team.getName(),
                team.getLeader().getId(),
                team.getLeader().getUserName(),
                memberCount,
                submission != null,
                submission != null ? SubmissionResponse.of(submission, techStacks) : null
        );
    }
}
