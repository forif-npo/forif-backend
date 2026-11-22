package org.forif_backend.domain.staffrecruitment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

import java.time.LocalDate;

/**
 * 지원자 한 명의 면접 시각.
 *
 * 면접은 한 타임에 한 명이라 회차 안에서 칸마다 한 명, 지원자마다 한 칸이다.
 * 면접 길이는 면접 일정의 칸 길이를 따르므로 시작 시각만 담는다.
 * 면접이 길어지거나 바뀌면 회장단이 다른 칸으로 옮긴다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_staff_interview_assignment")
public class StaffInterviewAssignment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interview_assignment_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private StaffRecruitment recruitment;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private StaffApplication application;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_minute", nullable = false)
    private int startMinute;

    private StaffInterviewAssignment(StaffRecruitment recruitment, StaffApplication application,
                                     LocalDate slotDate, int startMinute) {
        this.recruitment = recruitment;
        this.application = application;
        this.slotDate = slotDate;
        this.startMinute = startMinute;
    }

    public static StaffInterviewAssignment create(StaffRecruitment recruitment, StaffApplication application,
                                                  LocalDate slotDate, int startMinute) {
        return new StaffInterviewAssignment(recruitment, application, slotDate, startMinute);
    }

    public void moveTo(LocalDate slotDate, int startMinute) {
        this.slotDate = slotDate;
        this.startMinute = startMinute;
    }

    public boolean isAt(LocalDate date, int minute) {
        return this.slotDate.equals(date) && this.startMinute == minute;
    }
}
