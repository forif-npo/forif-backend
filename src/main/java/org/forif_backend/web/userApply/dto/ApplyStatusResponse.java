package org.forif_backend.web.userApply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "지원 상태 응답")
@Builder
public record ApplyStatusResponse(
        @Schema(description = "1순위 지원 가능 여부", example = "true")
        boolean canApplyPrimary,

        @Schema(description = "2순위 지원 가능 여부", example = "true")
        boolean canApplySecondary,

        @Schema(description = "이미 지원한 1순위 스터디명", example = "Spring Boot 스터디")
        String primaryStudyName,

        @Schema(description = "이미 지원한 2순위 스터디명", example = "React 스터디")
        String secondaryStudyName
) {
}
