package org.forif_backend.domain.study;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.domain.user.User;
import org.forif_backend.web.study.dto.CreateStudyApplyRequest;
import org.forif_backend.web.user.dto.StudyApplyRequest;

import java.util.ArrayList;
import java.util.List;

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

    @Column(length = 50)
    private String subTitle;

    private String thumbnailImage;

    @ManyToMany
    @JoinTable(
            name = "tb_study_apply_tag_mapping",
            joinColumns = @JoinColumn(name = "study_apply_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<StudyTag> tags = new ArrayList<>();

    private Boolean isOnline;

    @Column(length = 500)
    private String goal;

    @Column(length = 500)
    private String explanation;

    @Column(length = 50)
    private String startTime;

    @Column(length = 50)
    private String endTime;

    private Integer weekDay;

    @Column(length = 50)
    private String location;

    @Column(length = 50)
    private String locationDetail;

    private Integer difficulty;

    @Column(length = 100)
    private String selectionCriteria;

    private Integer capacity;

    private Boolean requiresInterview;

    private String interviewDate;

    private Integer acceptanceStatus;

    private Integer actYear;

    private Integer actSemester;

    public StudyApply(User primaryMentor, User secondaryMentor, String thumbnailImage, String studyName, String subTitle, List<StudyTag> tags, Boolean isOnline, String goal, String explanation, String startTime, String endTime, Integer weekDay, String location, String locationDetail, Integer difficulty, String selectionCriteria, Integer capacity, Boolean requiresInterview, String interviewDate) {
        this.primaryMentor = primaryMentor;
        this.secondaryMentor = secondaryMentor;
        this.thumbnailImage = thumbnailImage;
        this.studyName = studyName;
        this.subTitle = subTitle;
        this.tags = tags;
        this.isOnline = isOnline;
        this.goal = goal;
        this.explanation = explanation;
        this.startTime = startTime;
        this.endTime = endTime;
        this.weekDay = weekDay;
        this.location = location;
        this.locationDetail = locationDetail;
        this.difficulty = difficulty;
        this.selectionCriteria = selectionCriteria;
        this.capacity = capacity;
        this.requiresInterview = requiresInterview;
        this.interviewDate = interviewDate;
    }

    public static StudyApply create(User primaryMentor, CreateStudyApplyRequest request, List<StudyTag> tags, String thumbnailImage) {
        return new StudyApply(
                primaryMentor,
                null,
                thumbnailImage,
                request.title(),
                request.subTitle(),
                tags,
                request.isOnline(),
                request.goal(),
                request.explanation(),
                request.startTime(),
                request.endTime(),
                request.weekDay(),
                request.studyLocation(),
                request.studyLocationDetail(),
                request.difficulty(),
                request.selectionCriteria(),
                request.capacity(),
                request.requiresInterview(),
                request.interviewDate()
        );
    }
}