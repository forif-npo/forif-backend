package org.forif_backend.web.team.dto;

import org.forif_backend.domain.team.ForifTeam;

public record ForifTeamResponse(
        Long id,
        Long userId,
        String userName,
        String phoneNum,
        int actYear,
        int actSemester,
        String userTitle,
        String clubDepartment,
        String introTag,
        String selfIntro,
        String profImgUrl,
        Integer graduateYear
) {
    public static ForifTeamResponse from(ForifTeam forIfTeam) {
        return new ForifTeamResponse(
                forIfTeam.getId(),
                forIfTeam.getUser().getId(),
                forIfTeam.getUser().getUserName(),
                forIfTeam.getUser().getPhoneNum(),
                forIfTeam.getActYear(),
                forIfTeam.getActSemester(),
                forIfTeam.getUserTitle(),
                forIfTeam.getClubDepartment(),
                forIfTeam.getIntroTag(),
                forIfTeam.getSelfIntro(),
                forIfTeam.getProfImgUrl(),
                forIfTeam.getGraduateYear()
        );
    }
}
