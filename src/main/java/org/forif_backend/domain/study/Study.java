package org.forif_backend.domain.study;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.domain.user.User;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tb_study")
public class Study extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_id")
    private Integer id;

    @Column(nullable = false)
    private int actYear;

    @Column(nullable = false)
    private int actSemester;

    @Column(length = 50)
    private String studyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_mentor_id")
    private User primaryMentor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secondary_mentor_id")
    private User secondaryMentor;

//    @Column(length = 50)
//    private String primaryMentorName;
//
//    @Column(length = 50)
//    private String secondaryMentorName;

    @ManyToMany
    @JoinTable(
        name = "tb_study_tag_mapping",
        joinColumns = @JoinColumn(name = "study_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<StudyTag> tags = new ArrayList<>();

    @Column
    private RecruitStatus recruitStatus;

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

    private StudyDifficulty difficulty;

    @Column(length = 300)
    private String imgUrl;
}
