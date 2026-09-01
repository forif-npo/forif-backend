package org.forif_backend.domain.study;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.domain.user.User;

/** 스터디별 멘토 활동 확인서 발급 이력. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "tb_mentor_confirmation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"study_id", "mentor_id"})
)
public class MentorConfirmation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mentor_confirmation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor;

    @Column(length = 300, nullable = false)
    private String confirmationObjectKey;

    public static MentorConfirmation issue(Study study, User mentor, String confirmationObjectKey) {
        MentorConfirmation confirmation = new MentorConfirmation();
        confirmation.study = study;
        confirmation.mentor = mentor;
        confirmation.confirmationObjectKey = confirmationObjectKey;
        return confirmation;
    }

    public void reissue(String confirmationObjectKey) {
        this.confirmationObjectKey = confirmationObjectKey;
    }
}
