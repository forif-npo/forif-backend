package org.forif_backend.web.user.dto;

import lombok.Builder;
import org.forif_backend.application.user.dto.UserApplyInfo;

import java.time.ZonedDateTime;

@Builder
public record UserApplyResponse(
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
    public static UserApplyResponse toUserApplyResponse(UserApplyInfo userApplyInfo) {
        return UserApplyResponse.builder()
                .applierName(userApplyInfo.applierName())
                .applierStudentId(userApplyInfo.applierStudentId())
                .primaryStudyName(userApplyInfo.primaryStudyName())
                .secondaryStudyName(userApplyInfo.secondaryStudyName())
                .primaryStudyComment(userApplyInfo.primaryStudyComment())
                .secondaryStudyComment(userApplyInfo.secondaryStudyComment())
                .applyDate(userApplyInfo.applyDate())
                .primaryStudyStatus(userApplyInfo.primaryStudyStatus())
                .secondaryStudyStatus(userApplyInfo.secondaryStudyStatus())
                .build();
    }
}
