package org.forif_backend.web.product.dto;

import lombok.Builder;
import org.forif_backend.application.product.dto.ProductInfo;

import java.util.List;

@Builder
public record AdminProductResponse(
        Integer productId,
        String slug,
        String name,
        String oneLiner,
        String description,
        String status,
        String sourceType,
        String sourceLabel,
        List<String> tags,
        List<String> techStack,
        String serviceUrl,
        String githubUrl,
        int actYear,
        String rejectReason,
        String appliedAt,
        String applicantName,
        Long applicantId
) {
    public static AdminProductResponse from(ProductInfo info) {
        return AdminProductResponse.builder()
                .productId(info.productId())
                .slug(info.slug())
                .name(info.name())
                .oneLiner(info.oneLiner())
                .description(info.description())
                .status(info.status())
                .sourceType(info.sourceType())
                .sourceLabel(info.sourceLabel())
                .tags(info.tags())
                .techStack(info.techStack())
                .serviceUrl(info.serviceUrl())
                .githubUrl(info.githubUrl())
                .actYear(info.actYear())
                .rejectReason(info.rejectReason())
                .appliedAt(info.appliedAt())
                .applicantName(info.applicantName())
                .applicantId(info.applicantId())
                .build();
    }

    public static List<AdminProductResponse> fromList(List<ProductInfo> infos) {
        return infos.stream().map(AdminProductResponse::from).toList();
    }
}
