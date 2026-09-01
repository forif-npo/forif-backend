package org.forif_backend.application.product.dto;

import java.util.List;

/**
 * 운영진의 서비스 정보 수정 명령.
 * null인 필드는 변경하지 않으며, 빈 값(빈 문자열·빈 리스트)은 해당 항목을 비우는 것을 뜻한다.
 */
public record UpdateProductCommand(
        String name,
        String oneLiner,
        String description,
        String sourceLabel,
        List<String> tags,
        List<String> techStack,
        String serviceUrl,
        String githubUrl
) {
}
