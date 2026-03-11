package org.forif_backend.web.semesterSchedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CreateSemesterScheduleRequest {

    @NotNull
    private Integer actYear;

    @NotNull
    private Integer actSemester;

    @NotBlank
    @Size(max = 50)
    private String scheduleType;

    @NotNull
    private LocalDateTime scheduledAt;
}
