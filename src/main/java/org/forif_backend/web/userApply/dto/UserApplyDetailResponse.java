package org.forif_backend.web.userApply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.forif_backend.application.user.dto.ApplyDetailInfo;

@Schema(description = "신청서 상세 응답")
@Builder
public record UserApplyDetailResponse(
        @Schema(description = "지원 동기 전문", example = "이 스터디에 지원하는 이유는 백엔드 개발 역량을 키우기 위해서입니다...")
        String applyReason
) {
    public static UserApplyDetailResponse from(ApplyDetailInfo applyDetailInfo) {
        return UserApplyDetailResponse.builder().applyReason(applyDetailInfo.applyReason()).build();
    }
}
