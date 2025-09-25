package org.forif_backend.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_user_apply", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"apply_year", "apply_semester", "applier_id"})
})
public class UserApply extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_apply_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applier_id", nullable = false)
    private User applier;

    @Column(nullable = false)
    private int applyYear;

    @Column(nullable = false)
    private int applySemester;

    @Column(nullable = false)
    private int primaryStudy;

    @Column(length = 2000)
    private String primaryIntro;

    private Integer secondaryStudy;

    @Column(length = 2000)
    private String secondaryIntro;

    private Integer payStatus;

    private Integer primaryStatus;

    private Integer secondaryStatus;
}