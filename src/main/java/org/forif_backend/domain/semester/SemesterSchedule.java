package org.forif_backend.domain.semester;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;

import java.time.LocalDateTime;

/**
 * 학기별 일정 기간.
 *
 * 학기·단계당 한 행이다. 멘티 모집·수락/거절은 행이 없으면 닫히고, 그 외 단계는
 * 설정을 잊었다고 해서 동아리 운영이 멈추지 않도록 상시 개방이다(fail-open).
 */
@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "tb_semester_schedule",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_semester_schedule_phase",
                columnNames = {"act_year", "act_semester", "phase"}
        )
)
public class SemesterSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @Column(name = "act_year", nullable = false)
    private int actYear;

    @Column(name = "act_semester", nullable = false)
    private int actSemester;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 30)
    private SemesterPhase phase;

    /** 반열림 구간 [startsAt, endsAt). Asia/Seoul 벽시계 기준 */
    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    /** 마지막으로 이 기간을 수정한 회장단 학번 */
    @Column(name = "updated_by")
    private Long updatedBy;

    public static SemesterSchedule create(int actYear, int actSemester, SemesterPhase phase,
                                          LocalDateTime startsAt, LocalDateTime endsAt, Long updatedBy) {
        SemesterSchedule schedule = new SemesterSchedule();
        schedule.actYear = actYear;
        schedule.actSemester = actSemester;
        schedule.phase = phase;
        schedule.startsAt = startsAt;
        schedule.endsAt = endsAt;
        schedule.updatedBy = updatedBy;
        return schedule;
    }

    public void update(LocalDateTime startsAt, LocalDateTime endsAt, Long updatedBy) {
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.updatedBy = updatedBy;
    }

    /** 반열림 구간이므로 종료 시각 당일은 포함하지 않는다 */
    public boolean contains(LocalDateTime at) {
        return !at.isBefore(startsAt) && at.isBefore(endsAt);
    }

    public boolean notStartedAt(LocalDateTime at) {
        return at.isBefore(startsAt);
    }
}
