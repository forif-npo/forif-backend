package org.forif_backend.web.semesterSchedule.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UpdateSemesterScheduleRequest {

    private String scheduleType;

    private LocalDateTime scheduledAt;
}
