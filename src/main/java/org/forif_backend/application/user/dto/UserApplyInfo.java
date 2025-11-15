package org.forif_backend.application.user.dto;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record UserApplyInfo(
        String applierName,
        String applierStudentId,
        String primaryStudyName,
        String secondaryStudyName,
        String primaryStudyComment,
        String secondaryStudyComment,
        ZonedDateTime applyDate,
        String primaryStudyStatus,
        String secondaryStudyStatus
) {
}
