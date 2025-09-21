package org.forif_backend.domain.study;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.domain.user.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_study_apply")
public class StudyApply extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "apply_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_mentor_id")
    private User primaryMentor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secondary_mentor_id")
    private User secondaryMentor;

    @Column(length = 50)
    private String studyName;

    @Column(length = 100)
    private String tag;

    @Column(length = 300)
    private String oneLiner;

    @Column(length = 5000)
    private String explanation;

    @Column(length = 50)
    private String startTime;

    @Column(length = 50)
    private String endTime;

    private Integer weekDay;

    @Column(length = 50)
    private String location;

    private Integer difficulty;

    private Integer acceptanceStatus;

    private Integer actYear;

    private Integer actSemester;
}