package org.forif_backend.domain.dues;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.domain.user.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "tb_member_semester_check",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_semester_check",
                columnNames = {"user_id", "act_year", "act_semester"}
        )
)
public class MemberSemesterCheck extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_semester_check_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int actYear;

    @Column(nullable = false)
    private int actSemester;

    @Column(nullable = false)
    private boolean duesPaid;

    @Column(nullable = false)
    private boolean googleFormSubmitted;

    /**
     * 합격 결과는 유지하되, 이번 학기 활동부원 등록을 철회한 상태다.
     * 회비·구글폼 확인이 나중에 변경되어도 수강생으로 다시 등록되지 않는다.
     */
    @Column(nullable = false)
    private boolean registrationWithdrawn;

    public static MemberSemesterCheck create(User user, int actYear, int actSemester) {
        MemberSemesterCheck memberCheck = new MemberSemesterCheck();
        memberCheck.user = user;
        memberCheck.actYear = actYear;
        memberCheck.actSemester = actSemester;
        return memberCheck;
    }

    public void update(Boolean duesPaid, Boolean googleFormSubmitted) {
        if (duesPaid != null) {
            this.duesPaid = duesPaid;
        }
        if (googleFormSubmitted != null) {
            this.googleFormSubmitted = googleFormSubmitted;
        }
    }

    public void withdrawRegistration() {
        this.registrationWithdrawn = true;
    }
}
