package org.forif_backend.domain.hackathon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_hackathon_evaluation_score", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"evaluation_id", "criterion_id"})
})
public class HackathonEvaluationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_score_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private HackathonEvaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id", nullable = false)
    private HackathonEvaluationCriterion criterion;

    @Column(nullable = false)
    private int score;

    public static HackathonEvaluationScore create(HackathonEvaluation evaluation,
                                                  HackathonEvaluationCriterion criterion,
                                                  int score) {
        HackathonEvaluationScore evaluationScore = new HackathonEvaluationScore();
        evaluationScore.evaluation = evaluation;
        evaluationScore.criterion = criterion;
        evaluationScore.score = score;
        return evaluationScore;
    }
}
