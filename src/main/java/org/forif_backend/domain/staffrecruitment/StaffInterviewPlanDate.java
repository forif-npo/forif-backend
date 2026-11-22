package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 면접 후보 날짜 하나. 지원 화면 시간표의 한 열이 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_interview_plan_date")
public class StaffInterviewPlanDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interview_plan_date_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_plan_id", nullable = false)
    private StaffInterviewPlan plan;

    @Column(name = "interview_date", nullable = false)
    private LocalDate interviewDate;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private StaffInterviewPlanDate(LocalDate interviewDate, int displayOrder) {
        this.interviewDate = interviewDate;
        this.displayOrder = displayOrder;
    }

    static StaffInterviewPlanDate create(LocalDate interviewDate, int displayOrder) {
        return new StaffInterviewPlanDate(interviewDate, displayOrder);
    }

    void updateOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    void assignTo(StaffInterviewPlan plan) {
        this.plan = plan;
    }
}
