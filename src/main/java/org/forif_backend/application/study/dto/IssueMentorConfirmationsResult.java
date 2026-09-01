package org.forif_backend.application.study.dto;

import java.util.List;

public record IssueMentorConfirmationsResult(
        int successCount,
        int skippedCount,
        List<ItemResult> results
) {
    public record ItemResult(
            Long userId,
            String userName,
            boolean success,
            String message,
            String confirmationUrl
    ) {
    }
}
