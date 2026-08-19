package org.forif_backend.domain.study;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Setter;

import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.user.User;
import org.forif_backend.web.study.dto.CreateStudyApplyRequest;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "tb_study",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_study_autonomous_semester",
                columnNames = {"act_year", "act_semester", "autonomous_flag"}
        )
)
public class Study extends BaseTimeEntity {

    public static final String AUTONOMOUS_STUDY_NAME = "자율스터디";
    private static final String AUTONOMOUS_STUDY_ONE_LINER =
            "공통의 관심사로 원하는 분야를 자유롭게 공부하는 스터디";
    private static final String AUTONOMOUS_STUDY_EXPLANATION =
            "자율스터디는 공통의 관심사를 가진 부원들이 모여 자유롭게 원하는 분야를 공부하는 스터디입니다. "
                    + "자율스터디는 FORIF 인증서가 발급되지 않으며, 출석 체크 대상에도 포함되지 않고 정해진 수업 회차나 일정이 없습니다. "
                    + "자율부원은 정규 스터디를 수강하지 않고 FORIF에 등록한 부원으로, 정규스터디 영역 외에는 FORIF 부원으로서 다양한 혜택을 누릴 수 있습니다.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_id")
    private Integer id;

    /** 신청서 수정과 승인 요청이 경합할 때 마지막 저장이 상태를 되돌리지 않도록 보호한다. */
    @Version
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long version;

    @Column(nullable = false)
    private int actYear;

    @Column(nullable = false)
    private int actSemester;

    @Column(length = 50)
    private String studyName;

    /** 자율스터디만 true이며, 일반 스터디는 NULL로 보관한다. */
    @Setter(AccessLevel.NONE)
    @Column(name = "autonomous_flag")
    private Boolean autonomousFlag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_mentor_id")
    private User primaryMentor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secondary_mentor_id")
    private User secondaryMentor;

    @Column(length = 50)
    private String primaryMentorName;

    @Column(length = 50)
    private String secondaryMentorName;

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

    // StudyApply에서 통합된 필드들
    @Column(length = 50)
    private String subTitle;

    private String thumbnailImage;

    private Boolean isOnline;

    @Column(length = 3000)
    private String goal;

    @Column(length = 50)
    private String locationDetail;

    @Column(length = 100)
    private String selectionCriteria;

    private Integer capacity;

    private Boolean requiresInterview;

    private LocalDateTime interviewDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "study_status", nullable = false)
    private StudyStatus studyStatus = StudyStatus.PENDING; // 기본값을 PENDING으로 설정

    @Column(length = 1000)
    private String rejectReason; // 거절 사유 저장 컬럼

    /**
     * 해당 스터디 멘토 여부 확인 메서드
     * @param userId 유저 ID
     * @return 멘토 여부
     */
    public boolean isMentor(Long userId) {
        // 레거시 스터디는 멘토가 이름 문자열로만 남아 있고 유저 FK가 없을 수 있다
        boolean isPrimary = this.primaryMentor != null && this.primaryMentor.getId().equals(userId);
        boolean isSecondary = this.secondaryMentor != null && this.secondaryMentor.getId().equals(userId);

        return isPrimary || isSecondary;
    }

    /** 자율스터디는 출석 및 수료증 발급 대상이 아니다. */
    public boolean isAutonomousStudy() {
        return Boolean.TRUE.equals(this.autonomousFlag);
    }

    public static boolean isAutonomousStudyName(String studyName) {
        return AUTONOMOUS_STUDY_NAME.equals(studyName);
    }

    /**
     * 초기 스터디 생성 메서드
     * @param mentor 멘토 유저
     * @return 스터디 객체
     */
    public static Study createPendingStudy(User mentor, int actYear, int actSemester) {
        Study study = new Study();
        study.primaryMentor = mentor;
        study.primaryMentorName = mentor.getUserName();
        study.actYear = actYear;
        study.actSemester = actSemester;

        return study;
    }

    /**
     * 운영진이 학기별로 개설하는 자율스터디를 생성한다.
     * 자율스터디는 멘토 개설 신청을 거치지 않지만, 신청자를 관리할 운영진을 대표 멘토로 둔다.
     */
    public static Study createAutonomousStudy(User mentor, int actYear, int actSemester) {
        Study study = new Study();
        study.actYear = actYear;
        study.actSemester = actSemester;
        study.studyName = AUTONOMOUS_STUDY_NAME;
        study.autonomousFlag = true;
        study.primaryMentor = mentor;
        study.primaryMentorName = mentor.getUserName();
        study.oneLiner = AUTONOMOUS_STUDY_ONE_LINER;
        study.explanation = AUTONOMOUS_STUDY_EXPLANATION;
        study.studyStatus = StudyStatus.APPROVED;
        return study;
    }

    /**
     * DTO로부터 스터디 데이터를 반영하는 도메인 메서드
     * 최초 신청(Create)과 재요청(Re-apply) 시 공통으로 사용됩니다.
     */
    public void applyRequestData(CreateStudyApplyRequest request, List<StudyTag> tags, User secondaryMentor) {
        this.studyName = request.getTitle();
        this.subTitle = null;
        this.oneLiner = request.getOneLiner();
        this.goal = request.getGoal();
        this.explanation = request.getExplanation();
        this.isOnline = request.getIsOnline();
        this.location = request.getStudyLocation();
        this.locationDetail = request.getStudyLocationDetail();
        this.weekDay = request.getWeekDay();
        this.startTime = request.getStartTime();
        this.endTime = request.getEndTime();
        this.difficulty = StudyDifficulty.fromLevel(request.getDifficulty());
        this.selectionCriteria = request.getSelectionCriteria();
        this.capacity = request.getCapacity();
        this.requiresInterview = request.getRequiresInterview();
        this.interviewDate = request.getInterviewDate();

        // 연관 관계 설정
        this.tags = tags;
        this.primaryMentorName = this.primaryMentor.getUserName();
        this.secondaryMentor = secondaryMentor;
        this.secondaryMentorName = secondaryMentor != null ? secondaryMentor.getUserName() : null;
    }

    /**
     * 거절 처리: 상태를 REJECTED로 변경하고 사유를 기록합니다.
     */
    public void reject(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ForifException(ErrorCode.REJECT_REASON_REQUIRED);
        }
        this.studyStatus = StudyStatus.REJECTED;
        this.rejectReason = reason;
    }

    /**
     * 승인 처리: 상태를 APPROVED로 변경하고 이전 거절 사유는 초기화합니다.
     */
    public void approve() {
        this.studyStatus = StudyStatus.APPROVED;
        this.rejectReason = null;
    }

    /**
     * 재요청 처리: 거절된 상태에서만 재요청(신청자가 수정 후 제출)이 가능합니다.
     */
    public void reApply() {
        if (this.studyStatus != StudyStatus.REJECTED) {
            throw new ForifException(ErrorCode.REAPPLY_ONLY_FOR_REJECTED);
        }
        this.studyStatus = StudyStatus.RE_APPLIED;
    }
}
