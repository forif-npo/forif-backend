package org.forif_backend.application.product.dto;

import lombok.Builder;
import org.forif_backend.domain.product.Product;
import org.forif_backend.domain.product.ProductMember;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Builder
public record ProductInfo(
        Integer productId,
        String slug,
        String name,
        String oneLiner,
        String description,
        String status,
        String sourceType,
        String sourceLabel,
        String thumbnailUrl,
        List<String> tags,
        List<String> techStack,
        String serviceUrl,
        String githubUrl,
        int actYear,
        String rejectReason,
        String appliedAt,
        String applicantName,
        Long applicantId,
        List<MemberInfo> members
) {

    public record MemberInfo(String userName, String roleLabel) {
        public static MemberInfo from(ProductMember member) {
            return new MemberInfo(member.getUserName(), member.getRoleLabel());
        }
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static ProductInfo from(Product product) {
        return from(product, null);
    }

    public static ProductInfo from(Product product, String thumbnailUrl) {
        return ProductInfo.builder()
                .thumbnailUrl(thumbnailUrl)
                .productId(product.getId())
                .slug(product.getSlug())
                .name(product.getName())
                .oneLiner(product.getOneLiner())
                .description(product.getDescription())
                .status(product.getStatus().name())
                .sourceType(product.getSourceType().name())
                .sourceLabel(product.getSourceLabel())
                .tags(splitCsv(product.getTags()))
                .techStack(splitCsv(product.getTechStack()))
                .serviceUrl(product.getServiceUrl())
                .githubUrl(product.getGithubUrl())
                .actYear(product.getActYear())
                .rejectReason(product.getRejectReason())
                .appliedAt(product.getCreatedAt() != null
                        ? product.getCreatedAt().format(DATE_FORMAT)
                        : null)
                .applicantName(product.getApplicant().getUserName())
                .applicantId(product.getApplicant().getId())
                .members(product.getMembers().stream().map(MemberInfo::from).toList())
                .build();
    }

    private static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
