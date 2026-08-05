package org.forif_backend.web.semesterSchedule.dto;

import org.forif_backend.application.semesterSchedule.dto.SemesterScheduleDto;

import java.time.LocalDateTime;

public record SemesterScheduleResponse(
        Long id,
        int actYear,
        int actSemester,
        String scheduleType,
        LocalDateTime scheduledAt
) {
    public static SemesterScheduleResponse from(SemesterScheduleDto dto) {
        return new SemesterScheduleResponse(
                dto.id(),
                dto.actYear(),
                dto.actSemester(),
                dto.scheduleType(),
                dto.scheduledAt()
        );
    }
}
