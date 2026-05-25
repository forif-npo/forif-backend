package org.forif_backend.domain.hackathon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.domain.user.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_hackathon_team", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hackathon_id", "name"})
})
public class HackathonTeam extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hackathon_team_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private HackathonEvent hackathon;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 200)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    private Integer maxMembers;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TeamStatus status;

    public static HackathonTeam create(HackathonEvent hackathon, User leader, String name, String topic,
                                       String description, Integer maxMembers) {
        HackathonTeam team = new HackathonTeam();
        team.hackathon = hackathon;
        team.leader = leader;
        team.name = name;
        team.topic = topic;
        team.description = description;
        team.maxMembers = maxMembers;
        team.status = TeamStatus.FORMING;
        return team;
    }

    public void update(String name, String topic, String description, Integer maxMembers) {
        if (name != null) this.name = name;
        if (topic != null) this.topic = topic;
        if (description != null) this.description = description;
        if (maxMembers != null) this.maxMembers = maxMembers;
    }

    public void disband() {
        this.status = TeamStatus.DISBANDED;
    }

    public boolean isLeader(Long userId) {
        return this.leader.getId().equals(userId);
    }
}
