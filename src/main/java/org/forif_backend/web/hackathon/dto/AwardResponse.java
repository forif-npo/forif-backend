package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.HackathonAward;

public record AwardResponse(
        Long awardId,
        Long hackathonId,
        Long hackathonTeamId,
        String teamName,
        String awardName,
        Integer awardRank
) {
    public static AwardResponse from(HackathonAward award) {
        return new AwardResponse(
                award.getId(),
                award.getHackathon().getId(),
                award.getTeam().getId(),
                award.getTeam().getName(),
                award.getAwardName(),
                award.getAwardRank()
        );
    }
}
