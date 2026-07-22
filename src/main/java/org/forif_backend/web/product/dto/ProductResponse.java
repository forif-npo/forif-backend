package org.forif_backend.web.product.dto;

import lombok.Builder;
import org.forif_backend.application.product.dto.ProductInfo;

import java.util.List;

@Builder
public record ProductResponse(
        String slug,
        String name,
        String oneLiner,
        String status,
        String sourceType,
        String sourceLabel,
        List<String> tags,
        String thumbnailUrl,
        int actYear
) {
    public static ProductResponse from(ProductInfo info) {
        return ProductResponse.builder()
                .slug(info.slug())
                .name(info.name())
                .oneLiner(info.oneLiner())
                .status(info.status())
                .sourceType(info.sourceType())
                .sourceLabel(info.sourceLabel())
                .tags(info.tags())
                .thumbnailUrl(null) // 썸네일 업로드는 후속 작업
                .actYear(info.actYear())
                .build();
    }

    public static List<ProductResponse> fromList(List<ProductInfo> infos) {
        return infos.stream().map(ProductResponse::from).toList();
    }
}
