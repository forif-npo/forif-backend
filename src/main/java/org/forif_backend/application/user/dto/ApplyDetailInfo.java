package org.forif_backend.application.user.dto;

import lombok.Builder;

@Builder
public record ApplyDetailInfo(
        String applyReason
) {
}
