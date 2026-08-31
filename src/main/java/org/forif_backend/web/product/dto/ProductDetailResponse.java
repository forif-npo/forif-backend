package org.forif_backend.web.product.dto;

import lombok.Builder;
import org.forif_backend.application.product.dto.ProductInfo;

import java.util.List;

@Builder
public record ProductDetailResponse(
        String slug,
        String name,
        String oneLiner,
        String description,
        String status,
        String operationStatus,
        String sourceType,
        String sourceLabel,
        List<String> tags,
        String thumbnailUrl,
        int actYear,
        String serviceUrl,
        String githubUrl,
        List<String> techStack,
        List<MemberResponse> members,
        List<String> screenshots
) {
    public record MemberResponse(String userName, String roleLabel) {
    }

    public static ProductDetailResponse from(ProductInfo info) {
        return ProductDetailResponse.builder()
                .slug(info.slug())
                .name(info.name())
                .oneLiner(info.oneLiner())
                .description(info.description())
                .status(info.status())
                .operationStatus(info.operationStatus())
                .sourceType(info.sourceType())
                .sourceLabel(info.sourceLabel())
                .tags(info.tags())
                .thumbnailUrl(info.thumbnailUrl())
                .actYear(info.actYear())
                .serviceUrl(info.serviceUrl())
                .githubUrl(info.githubUrl())
                .techStack(info.techStack())
                .members(info.members().stream()
                        .map(m -> new MemberResponse(m.userName(), m.roleLabel()))
                        .toList())
                .screenshots(List.of()) // 스크린샷 업로드는 후속 작업
                .build();
    }
}
