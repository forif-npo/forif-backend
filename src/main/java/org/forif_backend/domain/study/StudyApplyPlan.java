package org.forif_backend.domain.study;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_study_apply_plan", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"apply_id", "week_num"})
})
public class StudyApplyPlan extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_apply_plan_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apply_id", nullable = false)
    private StudyApply studyApply;

    @Column(nullable = false)
    private int weekNum;

    @Column(length = 300)
    private String section;

    @Column(length = 1000)
    private String content;
}