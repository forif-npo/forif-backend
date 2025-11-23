package org.forif_backend.application.user.dto;

import lombok.Builder;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.user.UserApply;

import java.time.ZonedDateTime;

@Builder
public record UserApplyInfo(
        Long applyId,
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
    public static UserApplyInfo from(UserApply userApply) {
        return UserApplyInfo.builder()
                .applyId(userApply.getId())
                .primaryStudyComment(userApply.getPrimaryIntro())
                .secondaryStudyComment(userApply.getSecondaryIntro())
                .applyDate(userApply.getCreatedAt().atZone(DateUtils.ZONE_SEOUL))
                .primaryStudyStatus(userApply.getPrimaryStatus().name())
                .secondaryStudyStatus(userApply.getSecondaryStatus() != null ? userApply.getSecondaryStatus().name() : null)
                .applierStudentId(userApply.getApplier().getId().toString()) //TODO: 학번?
                .applierName(userApply.getApplier().getUserName())
                .primaryStudyName(userApply.getPrimaryStudyName())
                .secondaryStudyName(userApply.getSecondaryStudyName()).build();
    }
}
