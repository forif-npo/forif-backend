package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.domain.user.User;

import java.time.LocalDateTime;

/**
 * 모집 회차별 지원서 열람 권한.
 *
 * JWT role을 바꾸지 않고 이 행으로만 열람을 허용한다. 지원서 요청마다 현재 상태를 다시 확인하므로
 * 회수·만료 이후 다음 요청부터 즉시 거절된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_recruitment_reviewer")
public class StaffRecruitmentReviewer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_reviewer_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private StaffRecruitment recruitment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "granted_by", nullable = false)
    private Long grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private Long revokedBy;

    private StaffRecruitmentReviewer(StaffRecruitment recruitment, User user, Long grantedBy,
                                     LocalDateTime grantedAt, LocalDateTime expiresAt) {
        this.recruitment = recruitment;
        this.user = user;
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
    }

    public static StaffRecruitmentReviewer grant(StaffRecruitment recruitment, User user, Long grantedBy,
                                                 LocalDateTime grantedAt, LocalDateTime expiresAt) {
        return new StaffRecruitmentReviewer(recruitment, user, grantedBy, grantedAt, expiresAt);
    }

    /** 회수·만료된 권한을 같은 행에서 다시 부여한다. 이력은 새 부여 시각으로 갱신된다. */
    public void regrant(Long grantedBy, LocalDateTime grantedAt, LocalDateTime expiresAt) {
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = null;
        this.revokedBy = null;
    }

    /** 이미 회수된 권한을 다시 회수해도 결과가 같도록 처음 회수 시각을 유지한다. */
    public void revoke(Long revokedBy, LocalDateTime revokedAt) {
        if (this.revokedAt != null) {
            return;
        }
        this.revokedAt = revokedAt;
        this.revokedBy = revokedBy;
    }

    public StaffReviewerStatus status(LocalDateTime now) {
        if (this.revokedAt != null) {
            return StaffReviewerStatus.REVOKED;
        }
        if (!now.isBefore(this.expiresAt)) {
            return StaffReviewerStatus.EXPIRED;
        }
        return StaffReviewerStatus.ACTIVE;
    }

    public boolean isActive(LocalDateTime now) {
        return status(now).isActive();
    }
}
