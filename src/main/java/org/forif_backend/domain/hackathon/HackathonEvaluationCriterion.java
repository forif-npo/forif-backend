package org.forif_backend.domain.hackathon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_hackathon_evaluation_criterion", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hackathon_id", "display_order"}),
        @UniqueConstraint(columnNames = {"hackathon_id", "name"})
})
public class HackathonEvaluationCriterion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "criterion_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private HackathonEvent hackathon;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private int maxScore;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(nullable = false)
    private int displayOrder;

    public static HackathonEvaluationCriterion create(HackathonEvent hackathon, String name, String description,
                                                       Integer maxScore, BigDecimal weight, int displayOrder) {
        HackathonEvaluationCriterion criterion = new HackathonEvaluationCriterion();
        criterion.hackathon = hackathon;
        criterion.update(name, description, maxScore, weight, displayOrder);
        return criterion;
    }

    public void update(String name, String description, Integer maxScore, BigDecimal weight, Integer displayOrder) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (maxScore != null) this.maxScore = maxScore;
        if (weight != null) this.weight = weight;
        if (displayOrder != null) this.displayOrder = displayOrder;
    }
}
