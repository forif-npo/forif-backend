package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.HackathonTeam;
import org.forif_backend.domain.hackathon.TeamStatus;

import java.util.List;

public record TeamResponse(
        Long hackathonTeamId,
        Long hackathonId,
        String name,
        String topic,
        String description,
        Long leaderId,
        String leaderName,
        Integer maxMembers,
        long memberCount,
        TeamStatus status,
        List<TeamMemberResponse> members
) {
    public static TeamResponse of(HackathonTeam team, long memberCount, List<TeamMemberResponse> members) {
        return new TeamResponse(
                team.getId(),
                team.getHackathon().getId(),
                team.getName(),
                team.getTopic(),
                team.getDescription(),
                team.getLeader().getId(),
                team.getLeader().getUserName(),
                team.getMaxMembers(),
                memberCount,
                team.getStatus(),
                members
        );
    }
}
