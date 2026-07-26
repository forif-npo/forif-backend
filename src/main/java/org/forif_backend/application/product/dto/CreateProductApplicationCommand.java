package org.forif_backend.application.product.dto;

import org.forif_backend.domain.product.ProductSourceType;

import java.util.List;

public record CreateProductApplicationCommand(
        String name,
        String slug,
        String oneLiner,
        String description,
        ProductSourceType sourceType,
        String serviceUrl,
        String githubUrl,
        List<String> techStack,
        List<String> tags
) {
}
