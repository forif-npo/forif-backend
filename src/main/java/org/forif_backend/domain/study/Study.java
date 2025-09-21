package org.forif_backend.domain.study;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(length = 50)
    private String primaryMentorName;

    @Column(length = 50)
    private String secondaryMentorName;

    @Column(length = 100)
    private StudyTag tag;

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

    @Column(length = 300)
    private String imgUrl;
}
