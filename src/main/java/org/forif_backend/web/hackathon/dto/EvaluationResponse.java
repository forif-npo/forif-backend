package org.forif_backend.web.hackathon.dto;

import org.forif_backend.domain.hackathon.EvaluatorType;
import org.forif_backend.domain.hackathon.HackathonEvaluation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EvaluationResponse(
        Long evaluationId,
        Long hackathonId,
        Long targetTeamId,
        Long evaluatorId,
        EvaluatorType evaluatorType,
        BigDecimal totalScore,
        LocalDateTime evaluatedAt,
        List<EvaluationRequest.Score> scores
) {
    public static EvaluationResponse of(HackathonEvaluation evaluation, List<EvaluationRequest.Score> scores) {
        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getHackathon().getId(),
                evaluation.getTargetTeam().getId(),
                evaluation.getEvaluator().getId(),
                evaluation.getEvaluatorType(),
                evaluation.getTotalScore(),
                evaluation.getEvaluatedAt(),
                scores
        );
    }
}
