package org.forif_backend.web.semesterSchedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CreateSemesterScheduleRequest {

    @NotNull
    private int actYear;

    @NotNull
    private int actSemester;

    @NotBlank
    private String scheduleType;

    @NotNull
    private LocalDateTime scheduledAt;
}
