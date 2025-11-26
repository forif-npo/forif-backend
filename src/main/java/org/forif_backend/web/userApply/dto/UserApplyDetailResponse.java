package org.forif_backend.web.userApply.dto;

import lombok.Builder;
import org.forif_backend.application.user.dto.ApplyDetailInfo;

@Builder
public record UserApplyDetailResponse(
        String applyReason
) {
    public static UserApplyDetailResponse from(ApplyDetailInfo applyDetailInfo) {
        return UserApplyDetailResponse.builder().applyReason(applyDetailInfo.applyReason()).build();
    }
}
