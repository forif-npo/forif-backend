package org.forif_backend.application.semesterSchedule.dto;

import org.forif_backend.domain.common.SemesterSchedule;

import java.time.LocalDateTime;

public record SemesterScheduleDto(
        Long id,
        int actYear,
        int actSemester,
        String scheduleType,
        LocalDateTime scheduledAt
) {
    public static SemesterScheduleDto from(SemesterSchedule schedule) {
        return new SemesterScheduleDto(
                schedule.getId(),
                schedule.getActYear(),
                schedule.getActSemester(),
                schedule.getScheduleType(),
                schedule.getScheduledAt()
        );
    }
}
