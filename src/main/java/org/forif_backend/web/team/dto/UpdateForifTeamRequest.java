package org.forif_backend.web.team.dto;

import jakarta.validation.constraints.Size;

public record UpdateForifTeamRequest(
        @Size(max = 30)
        String userTitle,
        @Size(max = 30)
        String clubDepartment,
        @Size(max = 100)
        String introTag,
        @Size(max = 100)
        String selfIntro,
        Integer graduateYear
) {
}
