package org.forif_backend.web.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "수료증 발급 요청")
public record IssueCertificatesRequest(
        @Schema(description = "발급 대상 유저 ID(학번) 목록", example = "[2024097956, 2023012345]")
        @NotEmpty
        List<Long> userIds,

        @Schema(description = "수료증에 표기할 활동 기간", example = "2026.03.02.~2026.06.20.")
        @NotBlank
        String activityPeriod,

        @Schema(description = "자격(출석/해커톤) 미달자도 발급할지 여부. 운영진이 경고 확인 후 강제 발급할 때 사용", example = "false")
        Boolean ignoreEligibility
) {
}
