package org.forif_backend.domain.hackathon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_hackathon_award", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hackathon_id", "hackathon_team_id", "award_name"})
})
public class HackathonAward extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "award_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private HackathonEvent hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_team_id", nullable = false)
    private HackathonTeam team;

    @Column(length = 100, nullable = false)
    private String awardName;

    private Integer awardRank;

    public static HackathonAward create(HackathonEvent hackathon, HackathonTeam team, String awardName, Integer awardRank) {
        HackathonAward award = new HackathonAward();
        award.hackathon = hackathon;
        award.team = team;
        award.update(awardName, awardRank);
        return award;
    }

    public void update(String awardName, Integer awardRank) {
        if (awardName != null) this.awardName = awardName;
        if (awardRank != null) this.awardRank = awardRank;
    }
}
