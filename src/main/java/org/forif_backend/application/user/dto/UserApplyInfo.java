package org.forif_backend.application.user.dto;

import lombok.Builder;

@Builder
public record UserApplyInfo(
        String applierName,
        String applierId,
        String primaryStudyName,
        String secondaryStudyName,
        String applyComment,
        String applyDate,
        String applyStatus
) {
}
