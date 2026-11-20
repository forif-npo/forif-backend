package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.domain.team.ClubDepartment;

/**
 * 모집 회차의 지원 부서. 공통 운영진 부서를 참조하며 이름은 이 회차의 표시 값으로 보관한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_recruitment_department")
public class StaffRecruitmentDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_department_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private StaffRecruitment recruitment;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_department_id")
    private ClubDepartment clubDepartment;

    @Column(length = 200)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** 모집을 끝낸 부서를 숨기되 과거 지원서의 참조는 유지한다. */
    @Column(nullable = false)
    private boolean active;

    private StaffRecruitmentDepartment(String name, String description, int displayOrder, boolean active) {
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public static StaffRecruitmentDepartment create(String name, String description, int displayOrder, boolean active) {
        return new StaffRecruitmentDepartment(name, description, displayOrder, active);
    }

    /** 식별자를 유지한 채 내용만 바꾼다. 제출된 지원서의 부서 참조가 끊기지 않는다. */
    void update(String name, String description, int displayOrder, boolean active) {
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    void assignTo(StaffRecruitment recruitment) {
        this.recruitment = recruitment;
    }

    public void assignClubDepartment(ClubDepartment department) {
        this.clubDepartment = department;
        this.name = department.getName();
    }
}
