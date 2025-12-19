package org.forif_backend.domain.study;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_study_plan", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"study_id", "week_num"})
})
public class StudyPlan extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_plan_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @Column(nullable = false)
    private int weekNum;

    @Column
    private java.time.ZonedDateTime date;

    @Column(length = 300)
    private String section;

    @Column(length = 500)
    private String content;

    public StudyPlan(Study study, int weekNum, java.time.ZonedDateTime date, String topic, String content) {
        this.study = study;
        this.weekNum = weekNum;
        this.date = date;
        this.section = topic;
        this.content = content;
    }

    public static StudyPlan create(int weekNum, java.time.ZonedDateTime date, String topic, String content, Study study) {
        return new StudyPlan(study, weekNum, date, topic, content);
    }
}