package org.forif_backend.web.study.dto;

import java.util.List;
import org.forif_backend.application.study.dto.IssueMentorConfirmationsResult;

public record IssueMentorConfirmationsResponse(
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

    public static IssueMentorConfirmationsResponse from(IssueMentorConfirmationsResult result) {
        return new IssueMentorConfirmationsResponse(
                result.successCount(),
                result.skippedCount(),
                result.results().stream()
                        .map(item -> new ItemResult(
                                item.userId(), item.userName(), item.success(), item.message(),
                                item.confirmationUrl()))
                        .toList()
        );
    }
}
