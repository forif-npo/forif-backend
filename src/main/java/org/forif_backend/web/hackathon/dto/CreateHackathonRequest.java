package org.forif_backend.web.hackathon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.forif_backend.domain.hackathon.CompetitionType;

import java.time.LocalDateTime;

public record CreateHackathonRequest(
        @NotNull Integer heldYear,
        @NotNull Integer heldSemester,
        @NotNull Integer eventRound,
        @NotNull CompetitionType competitionType,
        @NotBlank String title,
        String description,
        String location,
        LocalDateTime recruitStartsAt,
        LocalDateTime recruitEndsAt,
        LocalDateTime teamBuildingStartsAt,
        LocalDateTime teamBuildingEndsAt,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt
) {
}
