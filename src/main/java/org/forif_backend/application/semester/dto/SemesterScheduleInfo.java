package org.forif_backend.application.semester.dto;

import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterSchedule;

import java.time.LocalDateTime;

public record SemesterScheduleInfo(
        Long id,
        int actYear,
        int actSemester,
        SemesterPhase phase,
        String phaseLabel,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean open
) {
    public static SemesterScheduleInfo from(SemesterSchedule schedule, LocalDateTime now) {
        return new SemesterScheduleInfo(
                schedule.getId(),
                schedule.getActYear(),
                schedule.getActSemester(),
                schedule.getPhase(),
                schedule.getPhase().getLabel(),
                schedule.getStartsAt(),
                schedule.getEndsAt(),
                schedule.contains(now)
        );
    }
}
