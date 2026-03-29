package org.forif_backend.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.common.util.DateUtils;
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

    public void addSecondaryStudy(Integer studyId, String studyName, String intro) {
        this.secondaryStudy = studyId;
        this.secondaryStudyName = studyName;
        this.secondaryIntro = intro;
        this.secondaryStatus = UserApplyStatus.PENDING;
    }

    public static UserApply applyStudy(User applier, Study primaryStudy, String applyReason) {
        return new UserApply(
                applier,
                DateUtils.getCurrentYear(),
                DateUtils.getCurrentSemester(),
                primaryStudy.getId(),
                applyReason,
                primaryStudy.getStudyName()
        );
    }
}
