package org.forif_backend.domain.hackathon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_hackathon_event", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"held_year", "held_semester", "event_round"})
})
public class HackathonEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hackathon_id")
    private Long id;

    @Column(nullable = false)
    private int heldYear;

    @Column(nullable = false)
    private int heldSemester;

    @Column(nullable = false)
    private int eventRound;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private HackathonStatus status;

    private LocalDateTime recruitStartsAt;
    private LocalDateTime recruitEndsAt;
    private LocalDateTime teamBuildingStartsAt;
    private LocalDateTime teamBuildingEndsAt;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime endsAt;

    private LocalDateTime deletedAt;

    public static HackathonEvent create(int heldYear, int heldSemester, int eventRound, String title,
                                        String description, String location,
                                        LocalDateTime recruitStartsAt, LocalDateTime recruitEndsAt,
                                        LocalDateTime teamBuildingStartsAt, LocalDateTime teamBuildingEndsAt,
                                        LocalDateTime startsAt, LocalDateTime endsAt) {
        HackathonEvent event = new HackathonEvent();
        event.heldYear = heldYear;
        event.heldSemester = heldSemester;
        event.eventRound = eventRound;
        event.title = title;
        event.description = description;
        event.location = location;
        event.status = HackathonStatus.RECRUITING;
        event.recruitStartsAt = recruitStartsAt;
        event.recruitEndsAt = recruitEndsAt;
        event.teamBuildingStartsAt = teamBuildingStartsAt;
        event.teamBuildingEndsAt = teamBuildingEndsAt;
        event.startsAt = startsAt;
        event.endsAt = endsAt;
        return event;
    }

    public void update(String title, String description, String location,
                       LocalDateTime recruitStartsAt, LocalDateTime recruitEndsAt,
                       LocalDateTime teamBuildingStartsAt, LocalDateTime teamBuildingEndsAt,
                       LocalDateTime startsAt, LocalDateTime endsAt) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (location != null) this.location = location;
        if (recruitStartsAt != null) this.recruitStartsAt = recruitStartsAt;
        if (recruitEndsAt != null) this.recruitEndsAt = recruitEndsAt;
        if (teamBuildingStartsAt != null) this.teamBuildingStartsAt = teamBuildingStartsAt;
        if (teamBuildingEndsAt != null) this.teamBuildingEndsAt = teamBuildingEndsAt;
        if (startsAt != null) this.startsAt = startsAt;
        if (endsAt != null) this.endsAt = endsAt;
    }

    public void changeStatus(HackathonStatus status) {
        this.status = status;
    }

    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
