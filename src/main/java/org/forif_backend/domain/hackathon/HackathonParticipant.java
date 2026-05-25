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
@Table(name = "tb_hackathon_participant", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hackathon_id", "user_id"})
})
public class HackathonParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private HackathonEvent hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private ParticipantStatus status;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    private LocalDateTime canceledAt;

    public static HackathonParticipant register(HackathonEvent hackathon, User user, LocalDateTime now) {
        HackathonParticipant participant = new HackathonParticipant();
        participant.hackathon = hackathon;
        participant.user = user;
        participant.status = ParticipantStatus.REGISTERED;
        participant.registeredAt = now;
        return participant;
    }

    public void registerAgain(LocalDateTime now) {
        this.status = ParticipantStatus.REGISTERED;
        this.registeredAt = now;
        this.canceledAt = null;
    }

    public void cancel(LocalDateTime now) {
        this.status = ParticipantStatus.CANCELED;
        this.canceledAt = now;
    }
}
