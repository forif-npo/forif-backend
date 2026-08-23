package org.forif_backend.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.study.Study;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_user_apply", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"apply_year", "apply_semester", "applier_id"})
})
public class UserApply extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_apply_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applier_id", nullable = false)
    private User applier;

    @Column(nullable = false)
    private int applyYear;

    @Column(nullable = false)
    private int applySemester;

    @Column(nullable = false)
    private int primaryStudy;

    @Column(nullable = false)
    private String primaryStudyName;

    @Column(length = 2000)
    private String primaryIntro;

    private Integer secondaryStudy;
    private String secondaryStudyName;

    @Column(length = 2000)
    private String secondaryIntro;

    private Integer payStatus;

    @Enumerated(EnumType.STRING)
    private UserApplyStatus primaryStatus;

    @Enumerated(EnumType.STRING)
    private UserApplyStatus secondaryStatus;

    private UserApply(User applier, int applyYear, int applySemester, int primaryStudy, String primaryIntro, String primaryStudyName) {
        this.applier = applier;
        this.applyYear = applyYear;
        this.applySemester = applySemester;
        this.primaryStudy = primaryStudy;
        this.primaryIntro = primaryIntro;
        this.primaryStudyName = primaryStudyName;
        this.primaryStatus = UserApplyStatus.PENDING;
    }

    public void updateStatus(Integer studyId, UserApplyStatus status) {
        if (this.primaryStudy == studyId) {
            this.primaryStatus = status;
        } else if (studyId.equals(this.secondaryStudy)) {
            this.secondaryStatus = status;
        }
    }

    /**
     * 합격 상태를 되돌린다. 부원을 명단에서 뺄 때 함께 호출한다.
     *
     * ACCEPT를 남겨두면 멘토가 다시 합격 처리해도 이미 승낙 상태라 걸러져 수강생이 복구되지 않고,
     * 회비 확인 시 이 지원서를 근거로 수강생이 되살아난다.
     */
    public void revertAcceptance() {
        if (this.primaryStatus == UserApplyStatus.ACCEPT) {
            this.primaryStatus = UserApplyStatus.REJECT;
        }
        if (this.secondaryStatus == UserApplyStatus.ACCEPT) {
            this.secondaryStatus = UserApplyStatus.REJECT;
        }
    }

    public void addSecondaryStudy(Integer studyId, String studyName, String intro) {
        requireDifferentFromPrimary(studyId);
        this.secondaryStudy = studyId;
        this.secondaryStudyName = studyName;
        this.secondaryIntro = intro;
        this.secondaryStatus = UserApplyStatus.PENDING;
    }

    /**
     * 2순위 지원을 취소한다. 1순위 지원 정보는 유지한다.
     */
    public void cancelSecondaryApplication() {
        this.secondaryStudy = null;
        this.secondaryStudyName = null;
        this.secondaryIntro = null;
        this.secondaryStatus = null;
    }

    /**
     * 1순위 지원을 취소하면서 2순위가 있으면 그 지원을 1순위로 승격한다.
     */
    public void promoteSecondaryToPrimary() {
        this.primaryStudy = this.secondaryStudy;
        this.primaryStudyName = this.secondaryStudyName;
        this.primaryIntro = this.secondaryIntro;
        this.primaryStatus = this.secondaryStatus;
        cancelSecondaryApplication();
    }

    public void updatePrimaryApplication(int studyId, String studyName, String applyReason) {
        if (this.primaryStatus != UserApplyStatus.PENDING) {
            throw new ForifException(ErrorCode.APPLY_NOT_PENDING);
        }
        requireDifferentFromSecondary(studyId);
        this.primaryStudy = studyId;
        this.primaryStudyName = studyName;
        this.primaryIntro = applyReason;
    }

    public void updateSecondaryApplication(int studyId, String studyName, String applyReason) {
        if (this.secondaryStatus != UserApplyStatus.PENDING) {
            throw new ForifException(ErrorCode.APPLY_NOT_PENDING);
        }
        requireDifferentFromPrimary(studyId);
        this.secondaryStudy = studyId;
        this.secondaryStudyName = studyName;
        this.secondaryIntro = applyReason;
    }

    private void requireDifferentFromPrimary(Integer studyId) {
        if (studyId.equals(this.primaryStudy)) {
            throw new ForifException(ErrorCode.DUPLICATE_STUDY_PRIORITY);
        }
    }

    private void requireDifferentFromSecondary(int studyId) {
        if (this.secondaryStudy != null && this.secondaryStudy == studyId) {
            throw new ForifException(ErrorCode.DUPLICATE_STUDY_PRIORITY);
        }
    }

    public static UserApply applyStudy(User applier, Study primaryStudy, String applyReason,
                                       int applyYear, int applySemester) {
        return new UserApply(
                applier,
                applyYear,
                applySemester,
                primaryStudy.getId(),
                applyReason,
                primaryStudy.getStudyName()
        );
    }
}
