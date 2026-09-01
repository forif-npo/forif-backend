package org.forif_backend.web.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.forif_backend.application.study.dto.IssueCertificatesResult;

import java.util.List;

@Schema(description = "수료증 발급 처리 응답")
@Builder
public record IssueCertificatesResponse(
        @Schema(description = "발급 성공 수", example = "12")
        int successCount,

        @Schema(description = "스킵(자격 미달 등) 수", example = "2")
        int skippedCount,

        @Schema(description = "유저별 처리 결과")
        List<ItemResult> results
) {
    @Builder
    public record ItemResult(
            @Schema(description = "유저 ID(학번)", example = "2024097956")
            Long userId,

            @Schema(description = "이름", example = "홍길동")
            String userName,

            @Schema(description = "발급 성공 여부", example = "true")
            boolean success,

            @Schema(description = "처리 메시지", example = "발급 완료")
            String message,

            @Schema(description = "발급된 수료증 URL")
            String certificateUrl
    ) {
    }

    public static IssueCertificatesResponse from(IssueCertificatesResult result) {
        return IssueCertificatesResponse.builder()
                .successCount(result.successCount())
                .skippedCount(result.skippedCount())
                .results(result.results().stream()
                        .map(r -> ItemResult.builder()
                                .userId(r.userId())
                                .userName(r.userName())
                                .success(r.success())
                                .message(r.message())
                                .certificateUrl(r.certificateUrl())
                                .build())
                        .toList())
                .build();
    }
}
