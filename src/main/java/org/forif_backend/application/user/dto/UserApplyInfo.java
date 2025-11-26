package org.forif_backend.application.user.dto;

import lombok.Builder;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.user.UserApply;

import java.time.ZonedDateTime;

@Builder
public record UserApplyInfo(
        Long applyId,
        String applierName,
        String studyName,
        String studyComment,
        ZonedDateTime applyDate,
        String studyStatus
) {
    public static UserApplyInfo from(UserApply userApply, Study study) {
        return UserApplyInfo.builder()
                .applyId(userApply.getId())
                .applyDate(userApply.getCreatedAt().atZone(DateUtils.ZONE_SEOUL))
                .applierName(userApply.getApplier().getUserName())
                .studyName(study.getStudyName())
                .studyComment(study.getId() == userApply.getPrimaryStudy() ? userApply.getPrimaryIntro() : userApply.getSecondaryIntro())
                .studyStatus(study.getId() == userApply.getPrimaryStudy() ? userApply.getPrimaryStatus().getStatusName() : userApply.getSecondaryStatus().getStatusName())
                .build();
    }
}
