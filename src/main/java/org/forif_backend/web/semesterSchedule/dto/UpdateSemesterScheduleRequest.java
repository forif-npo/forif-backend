package org.forif_backend.web.semesterSchedule.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UpdateSemesterScheduleRequest {

    @Size(max = 50)
    private String scheduleType;

    private LocalDateTime scheduledAt;
}
