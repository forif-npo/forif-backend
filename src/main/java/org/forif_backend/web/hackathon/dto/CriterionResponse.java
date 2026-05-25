package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.HackathonEvaluationCriterion;

import java.math.BigDecimal;

public record CriterionResponse(
        Long criterionId,
        Long hackathonId,
        String name,
        String description,
        int maxScore,
        BigDecimal weight,
        int displayOrder
) {
    public static CriterionResponse from(HackathonEvaluationCriterion criterion) {
        return new CriterionResponse(
                criterion.getId(),
                criterion.getHackathon().getId(),
                criterion.getName(),
                criterion.getDescription(),
                criterion.getMaxScore(),
                criterion.getWeight(),
                criterion.getDisplayOrder()
        );
    }
}
