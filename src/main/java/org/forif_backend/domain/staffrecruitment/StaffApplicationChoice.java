package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원서의 지망 부서. 1지망은 필수, 2지망은 선택이며 같은 부서를 두 번 고를 수 없다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_application_choice")
public class StaffApplicationChoice {

    public static final int FIRST_PRIORITY = 1;
    public static final int SECOND_PRIORITY = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_choice_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private StaffApplication application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_department_id", nullable = false)
    private StaffRecruitmentDepartment department;

    @Column(nullable = false)
    private int priority;

    private StaffApplicationChoice(StaffRecruitmentDepartment department, int priority) {
        this.department = department;
        this.priority = priority;
    }

    public static StaffApplicationChoice create(StaffRecruitmentDepartment department, int priority) {
        return new StaffApplicationChoice(department, priority);
    }

    void assignTo(StaffApplication application) {
        this.application = application;
    }
}
