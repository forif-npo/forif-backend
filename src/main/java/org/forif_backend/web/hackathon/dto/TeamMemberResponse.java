package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.HackathonTeamMember;
import org.forif_backend.domain.hackathon.TeamMemberRole;

import java.time.LocalDateTime;

public record TeamMemberResponse(
        Long userId,
        String userName,
        TeamMemberRole role,
        LocalDateTime joinedAt
) {
    public static TeamMemberResponse from(HackathonTeamMember member) {
        return new TeamMemberResponse(
                member.getUser().getId(),
                member.getUser().getUserName(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
