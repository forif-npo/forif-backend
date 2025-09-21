package org.forif_backend.domain.team;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.domain.user.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_forif_team", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"act_year", "act_semester", "user_id"})
})
public class ForifTeam extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forif_team_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int actYear;

    @Column(nullable = false)
    private int actSemester;

    private Integer graduateYear;

    @Column(length = 30)
    private String userTitle;

    @Column(length = 30)
    private String clubDepartment;

    @Column(length = 100)
    private String introTag;

    @Column(length = 100)
    private String selfIntro;

    @Column(length = 300)
    private String profImgUrl;
}