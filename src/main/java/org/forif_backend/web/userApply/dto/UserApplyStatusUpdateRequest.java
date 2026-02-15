package org.forif_backend.web.userApply.dto;

import jakarta.validation.constraints.NotNull;
import org.forif_backend.domain.user.UserApplyStatus;

public record UserApplyStatusUpdateRequest(
        @NotNull UserApplyStatus status,
        Integer waitlistOrder
) {
}
