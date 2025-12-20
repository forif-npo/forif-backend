package org.forif_backend.web.userApply.dto;

import lombok.Builder;
import org.forif_backend.application.user.dto.UserApplyInfo;

import java.time.ZonedDateTime;

@Builder
public record UserApplyResponse(
        Long applyId,
        String applierName,
        String studyName,
        String studyComment,
        ZonedDateTime applyDate,
        String studyStatus
) {
    public static UserApplyResponse from(UserApplyInfo userApplyInfo) {
        return UserApplyResponse.builder()
                .applyId(userApplyInfo.applyId())
                .applierName(userApplyInfo.applierName())
                .applyDate(userApplyInfo.applyDate())
                .studyName(userApplyInfo.studyName())
                .studyComment(userApplyInfo.studyComment())
                .studyStatus(userApplyInfo.studyStatus())
                .build();
    }
}
