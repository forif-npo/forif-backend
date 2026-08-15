package org.forif_backend.web.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record IssueMentorConfirmationsRequest(
        @NotEmpty List<Long> userIds,
        @NotBlank String activityPeriod
) {
}
