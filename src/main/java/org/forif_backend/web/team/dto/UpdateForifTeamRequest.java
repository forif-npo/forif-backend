package org.forif_backend.web.team.dto;

public record UpdateForifTeamRequest(
        String userTitle,
        String clubDepartment,
        String introTag,
        String selfIntro,
        String profImgUrl,
        Integer graduateYear
) {
}
