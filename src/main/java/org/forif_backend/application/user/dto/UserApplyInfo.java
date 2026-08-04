package org.forif_backend.application.user.dto;

import lombok.Builder;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.user.UserApply;

import java.time.LocalDateTime;

@Builder
public record UserApplyInfo(
        Long applyId,
        String applierName,
        String department,
        String studyName,
        String studyComment,
        LocalDateTime applyDate,
        String studyStatus,
        int priority
) {
    public static UserApplyInfo from(UserApply userApply, Study study) {
        int priority = study.getId() == userApply.getPrimaryStudy() ? 1 : 2;
        return UserApplyInfo.builder()
                .applyId(userApply.getId())
                .applyDate(userApply.getCreatedAt())
                .applierName(userApply.getApplier().getUserName())
                .department(userApply.getApplier().getDepartment())
                .studyName(study.getStudyName())
                .studyComment(study.getId() == userApply.getPrimaryStudy() ? userApply.getPrimaryIntro() : userApply.getSecondaryIntro())
                .studyStatus(study.getId() == userApply.getPrimaryStudy() ? userApply.getPrimaryStatus().getStatusName() : userApply.getSecondaryStatus().getStatusName())
                .priority(priority)
                .build();
    }
}
