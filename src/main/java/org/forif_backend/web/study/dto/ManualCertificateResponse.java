package org.forif_backend.web.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수료증 수동 발급 응답")
public record ManualCertificateResponse(
        @Schema(description = "생성된 수료증 URL")
        String certificateUrl
) {
}
