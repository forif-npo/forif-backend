package org.forif_backend.application.study.dto;

import lombok.Builder;

import java.util.List;

/**
 * 수료증 발급 처리 결과
 */
@Builder
public record IssueCertificatesResult(
        int successCount,
        int skippedCount,
        List<ItemResult> results
) {
    @Builder
    public record ItemResult(
            Long userId,
            String userName,
            boolean success,
            String message,
            String certificateUrl
    ) {
    }
}
