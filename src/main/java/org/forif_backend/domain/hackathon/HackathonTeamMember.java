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
@Table(name = "tb_hackathon_team_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hackathon_id", "user_id"}),
        @UniqueConstraint(columnNames = {"hackathon_team_id", "user_id"})
})
public class HackathonTeamMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_member_id")
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
    private TeamMemberRole role;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    public static HackathonTeamMember createLeader(HackathonEvent hackathon, HackathonTeam team, User user, LocalDateTime now) {
        return create(hackathon, team, user, TeamMemberRole.LEADER, now);
    }

    public static HackathonTeamMember createMember(HackathonEvent hackathon, HackathonTeam team, User user, LocalDateTime now) {
        return create(hackathon, team, user, TeamMemberRole.MEMBER, now);
    }

    private static HackathonTeamMember create(HackathonEvent hackathon, HackathonTeam team, User user,
                                              TeamMemberRole role, LocalDateTime now) {
        HackathonTeamMember member = new HackathonTeamMember();
        member.hackathon = hackathon;
        member.team = team;
        member.user = user;
        member.role = role;
        member.joinedAt = now;
        return member;
    }
}
