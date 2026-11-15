package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 운영진 지원서.
 *
 * 제출 당시의 개인정보를 값으로 보관한다. 회원이 프로필을 바꾸거나 탈퇴해도 제출 내용은 그대로 남는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_application")
public class StaffApplication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private StaffRecruitment recruitment;

    /** 탈퇴하면 null이 된다. 식별은 snapshot으로 남는다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_user_id")
    private User applicant;

    @Column(name = "applicant_name", nullable = false, length = 50)
    private String applicantName;

    @Column(name = "applicant_email", nullable = false, length = 100)
    private String applicantEmail;

    @Column(name = "applicant_phone_num", length = 20)
    private String applicantPhoneNum;

    @Column(name = "applicant_department", length = 100)
    private String applicantDepartment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffApplicationStatus status;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("priority ASC")
    private List<StaffApplicationChoice> choices = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<StaffApplicationAnswer> answers = new ArrayList<>();

    private StaffApplication(StaffRecruitment recruitment, User applicant, String applicantName,
                             String applicantEmail, String applicantPhoneNum, String applicantDepartment,
                             LocalDateTime submittedAt) {
        this.recruitment = recruitment;
        this.applicant = applicant;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.applicantPhoneNum = applicantPhoneNum;
        this.applicantDepartment = applicantDepartment;
        this.status = StaffApplicationStatus.SUBMITTED;
        this.submittedAt = submittedAt;
    }

    public static StaffApplication submit(StaffRecruitment recruitment, User applicant, String applicantName,
                                          String applicantEmail, String applicantPhoneNum,
                                          String applicantDepartment, LocalDateTime submittedAt) {
        if (!recruitment.isApplicationOpen(submittedAt)) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_OPEN);
        }
        return new StaffApplication(recruitment, applicant, applicantName, applicantEmail,
                applicantPhoneNum, applicantDepartment, submittedAt);
    }

    public void addChoice(StaffApplicationChoice choice) {
        choice.assignTo(this);
        this.choices.add(choice);
    }

    public void addAnswer(StaffApplicationAnswer answer) {
        answer.assignTo(this);
        this.answers.add(answer);
    }

    /** 본인 지원서인지. ID 추측으로 타인 지원서를 열람하지 못하게 한다. */
    public boolean isOwnedBy(Long userId) {
        return this.applicant != null && this.applicant.getId().equals(userId);
    }
}
