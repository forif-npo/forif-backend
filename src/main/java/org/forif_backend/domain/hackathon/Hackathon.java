package org.forif_backend.domain.hackathon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_hackathon", uniqueConstraints = {
        // '특정 년도/학기'의 '특정 팀ID'는 유일해야 함을 보장
        @UniqueConstraint(columnNames = {"held_year", "held_semester", "team_id"})
})
public class Hackathon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hackathon_team_id")
    private Integer id;

    @Column(nullable = false)
    private int heldYear;

    @Column(nullable = false)
    private int heldSemester;

    @Column(nullable = false)
    private int teamId;

    @Column(length = 80)
    private String projectName;

    @Column(length = 300)
    private String resultUrl;
}