package org.forif_backend.domain.hackathon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.domain.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_hackathon_evaluation", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hackathon_id", "target_team_id", "evaluator_id"})
})
public class HackathonEvaluation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private HackathonEvent hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_team_id", nullable = false)
    private HackathonTeam targetTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private User evaluator;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private EvaluatorType evaluatorType;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal totalScore;

    @Column(nullable = false)
    private LocalDateTime evaluatedAt;

    public static HackathonEvaluation create(HackathonEvent hackathon, HackathonTeam targetTeam, User evaluator,
                                             EvaluatorType evaluatorType, BigDecimal totalScore, LocalDateTime now) {
        HackathonEvaluation evaluation = new HackathonEvaluation();
        evaluation.hackathon = hackathon;
        evaluation.targetTeam = targetTeam;
        evaluation.evaluator = evaluator;
        evaluation.evaluatorType = evaluatorType;
        evaluation.totalScore = totalScore;
        evaluation.evaluatedAt = now;
        return evaluation;
    }

    public void update(BigDecimal totalScore, LocalDateTime now) {
        this.totalScore = totalScore;
        this.evaluatedAt = now;
    }
}
