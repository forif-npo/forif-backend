package org.forif_backend.domain.hackathon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.domain.user.User;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_hackathon_join_request")
public class HackathonJoinRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "join_request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private HackathonEvent hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_team_id", nullable = false)
    private HackathonTeam team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private JoinRequestStatus status;

    @Column(length = 500)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    public static HackathonJoinRequest create(HackathonEvent hackathon, HackathonTeam team, User user, String message) {
        HackathonJoinRequest request = new HackathonJoinRequest();
        request.hackathon = hackathon;
        request.team = team;
        request.user = user;
        request.message = message;
        request.status = JoinRequestStatus.PENDING;
        return request;
    }

    public void approve(User reviewer, LocalDateTime now) {
        this.status = JoinRequestStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewedAt = now;
    }

    public void reject(User reviewer, LocalDateTime now) {
        this.status = JoinRequestStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewedAt = now;
    }

    public void cancel() {
        this.status = JoinRequestStatus.CANCELED;
    }
}
