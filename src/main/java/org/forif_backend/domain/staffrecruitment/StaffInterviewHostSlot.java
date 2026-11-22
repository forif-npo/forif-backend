package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 회장단이 면접을 볼 수 있는 시간 한 칸.
 *
 * 회장단이 함께 면접을 보므로 사람별이 아니라 회차마다 하나의 시간표로 둔다.
 * 지원자 선택과 같이 날짜와 시작 분(자정 기준)을 값으로 담는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_interview_host_slot")
public class StaffInterviewHostSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interview_host_slot_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private StaffRecruitment recruitment;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_minute", nullable = false)
    private int startMinute;

    private StaffInterviewHostSlot(StaffRecruitment recruitment, LocalDate slotDate, int startMinute) {
        this.recruitment = recruitment;
        this.slotDate = slotDate;
        this.startMinute = startMinute;
    }

    public static StaffInterviewHostSlot create(StaffRecruitment recruitment, LocalDate slotDate, int startMinute) {
        return new StaffInterviewHostSlot(recruitment, slotDate, startMinute);
    }

    public boolean isAt(LocalDate date, int minute) {
        return this.slotDate.equals(date) && this.startMinute == minute;
    }
}
