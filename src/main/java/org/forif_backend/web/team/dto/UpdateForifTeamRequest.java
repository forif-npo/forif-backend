package org.forif_backend.web.team.dto;

import lombok.Getter;

@Getter
public class UpdateForifTeamRequest {

    private String userTitle;
    private String clubDepartment;
    private String introTag;
    private String selfIntro;
    private String profImgUrl;
    private Integer graduateYear;
}
