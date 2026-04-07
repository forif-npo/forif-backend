package org.forif_backend.web.userApply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.forif_backend.web.study.dto.StudyResponse;

@Schema(description = "지원 상태 응답")
@Builder
public record ApplyStatusResponse(
        @Schema(description = "1순위 지원 가능 여부", example = "true")
        boolean canApplyPrimary,

        @Schema(description = "2순위 지원 가능 여부", example = "true")
        boolean canApplySecondary,

        @Schema(description = "이미 지원한 1순위 스터디 정보")
        StudyResponse primaryStudy,

        @Schema(description = "이미 지원한 2순위 스터디 정보")
        StudyResponse secondaryStudy
) {
}
