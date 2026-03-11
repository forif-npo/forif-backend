package org.forif_backend.domain.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tb_semester_schedule")
public class SemesterSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @Column(nullable = false)
    private int actYear;

    @Column(nullable = false)
    private int actSemester;

    @Column(nullable = false, length = 50)
    private String scheduleType;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    public static SemesterSchedule create(int actYear, int actSemester, String scheduleType, LocalDateTime scheduledAt) {
        SemesterSchedule schedule = new SemesterSchedule();
        schedule.actYear = actYear;
        schedule.actSemester = actSemester;
        schedule.scheduleType = scheduleType;
        schedule.scheduledAt = scheduledAt;
        return schedule;
    }

    public void update(String scheduleType, LocalDateTime scheduledAt) {
        if (scheduleType != null && !scheduleType.isBlank()) this.scheduleType = scheduleType;
        if (scheduledAt != null) this.scheduledAt = scheduledAt;
    }
}
