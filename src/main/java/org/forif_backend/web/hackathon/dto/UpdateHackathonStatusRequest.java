package org.forif_backend.web.hackathon.dto;

import jakarta.validation.constraints.NotNull;
import org.forif_backend.domain.hackathon.HackathonStatus;

public record UpdateHackathonStatusRequest(
        @NotNull HackathonStatus status
) {
}
