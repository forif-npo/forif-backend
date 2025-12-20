package org.forif_backend.web.notification.dto;

import java.util.List;

public record SendAlimTalkResponse(
        int totalCount,          // 전체 발송 건수
        int successCount,        // 성공 건수
        int failureCount,        // 실패 건수
        List<String> results     // 상세 결과
) {
}