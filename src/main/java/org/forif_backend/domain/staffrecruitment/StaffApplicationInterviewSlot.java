package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 지원자가 고른 면접 가능 시간 한 칸.
 * 칸 길이는 회차의 {@link StaffInterviewPlan#getSlotMinutes()} 가 정하고,
 * 여기에는 시작 시각만 자정부터의 분으로 담는다.
 *
 * 후보 날짜 행을 참조하지 않고 날짜와 시간을 값으로 보관한다.
 * 제출 뒤 회장단이 후보 날짜를 바꿔도 지원자가 고른 내용이 사라지면 안 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_application_interview_slot")
public class StaffApplicationInterviewSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_interview_slot_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private StaffApplication application;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_minute", nullable = false)
    private int startMinute;

    private StaffApplicationInterviewSlot(LocalDate slotDate, int startMinute) {
        this.slotDate = slotDate;
        this.startMinute = startMinute;
    }

    public static StaffApplicationInterviewSlot create(LocalDate slotDate, int startMinute) {
        return new StaffApplicationInterviewSlot(slotDate, startMinute);
    }

    void assignTo(StaffApplication application) {
        this.application = application;
    }
}
