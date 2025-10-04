package org.forif_backend.domain.study;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.web.study.dto.CreateStudyApplyRequest;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_study_apply_plan", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"apply_id", "week_num"})
})
public class StudyApplyPlan extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "study_apply_plan_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apply_id", nullable = false)
    private StudyApply studyApply;

    @Column(nullable = false)
    private int weekNum;

    @Column(nullable = false)
    private String date;

    @Column(length = 300)
    private String topic;

    @Column(length = 1000)
    private String content;

    public StudyApplyPlan(StudyApply studyApply, int weekNum, String date, String topic, String content) {
        this.studyApply = studyApply;
        this.weekNum = weekNum;
        this.date = date;
        this.topic = topic;
        this.content = content;
    }

    public static StudyApplyPlan create(CreateStudyApplyRequest.Plan studyPlan, StudyApply studyApply) {
        return new StudyApplyPlan(
                studyApply,
                studyPlan.weekNum(),
                studyPlan.date(),
                studyPlan.topic(),
                studyPlan.content()
        );
    }
}