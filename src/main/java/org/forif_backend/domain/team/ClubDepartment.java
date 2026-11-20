package org.forif_backend.domain.team;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 운영진 이력과 모집 회차가 함께 참조하는 부서. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_club_department")
public class ClubDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_department_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    private ClubDepartment(String name) {
        this.name = name;
    }

    public static ClubDepartment create(String name) {
        return new ClubDepartment(name);
    }
}
