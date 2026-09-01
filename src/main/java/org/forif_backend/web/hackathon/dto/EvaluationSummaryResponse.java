package org.forif_backend.web.hackathon.dto;

import java.math.BigDecimal;
import java.util.List;

public record EvaluationSummaryResponse(
        Long teamId,
        String teamName,
        BigDecimal averageTotalScore,
        BigDecimal sumTotalScore,
        long evaluatorCount,
        List<CriterionAverage> criterionAverages
) {
    public record CriterionAverage(
            Long criterionId,
            String name,
            BigDecimal averageScore
    ) {
    }
}
