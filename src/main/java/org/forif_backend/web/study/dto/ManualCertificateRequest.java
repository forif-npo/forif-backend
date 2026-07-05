package org.forif_backend.web.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "수료증 수동 발급 요청 (모든 표기 정보를 직접 입력)")
public record ManualCertificateRequest(
        @Schema(description = "이름", example = "홍길동")
        @NotBlank
        String userName,

        @Schema(description = "학번", example = "2024097956")
        @NotBlank
        String studentNumber,

        @Schema(description = "학과", example = "정보시스템학과")
        @NotBlank
        String department,

        @Schema(description = "스터디명(수료 과정명)", example = "README.md")
        @NotBlank
        String studyName,

        @Schema(description = "활동 기간", example = "2026.03.02.~2026.06.20.")
        @NotBlank
        String activityPeriod,

        @Schema(description = "발급일 (미입력 시 오늘 날짜)", example = "2026. 07. 07.")
        String issueDate,

        @Schema(description = "회장 이름 (미입력 시 현재 회장, 과거 재발행 시 당시 회장 지정 가능)", example = "권기태")
        String presidentName
) {
}
