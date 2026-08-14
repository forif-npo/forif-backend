package org.forif_backend.web.product.dto;

import lombok.Builder;
import org.forif_backend.application.product.dto.ProductInfo;

import java.util.List;

@Builder
public record ProductApplicationResponse(
        Integer applicationId,
        String name,
        String slug,
        String oneLiner,
        String description,
        String sourceType,
        String serviceUrl,
        String githubUrl,
        String thumbnailUrl,
        List<String> techStack,
        String status,
        String rejectReason,
        String appliedAt
) {
    public static ProductApplicationResponse from(ProductInfo info) {
        return ProductApplicationResponse.builder()
                .applicationId(info.productId())
                .name(info.name())
                .slug(info.slug())
                .oneLiner(info.oneLiner())
                .description(info.description())
                .sourceType(info.sourceType())
                .serviceUrl(info.serviceUrl())
                .githubUrl(info.githubUrl())
                .thumbnailUrl(info.thumbnailUrl())
                .techStack(info.techStack())
                .status(toApplicationStatus(info.status()))
                .rejectReason(info.rejectReason())
                .appliedAt(info.appliedAt())
                .build();
    }

    public static List<ProductApplicationResponse> fromList(List<ProductInfo> infos) {
        return infos.stream().map(ProductApplicationResponse::from).toList();
    }

    /** 게시 상태(LIVE/DEV/...)는 신청자 관점에서 모두 "승인"으로 표기 */
    private static String toApplicationStatus(String status) {
        return switch (status) {
            case "PENDING" -> "PENDING";
            case "REJECTED" -> "REJECTED";
            default -> "APPROVED";
        };
    }
}
