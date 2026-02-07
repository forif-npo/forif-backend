package org.forif_backend.application.study.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.StudyPlan;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
@Builder
public class StudyPlanDto {
    private final Long id;
    private final int weekNum;
    private final LocalDateTime date;
    private final String section;
    private final String content;

    public static StudyPlanDto from(StudyPlan plan) {
        return StudyPlanDto.builder()
                .id(plan.getId())
                .weekNum(plan.getWeekNum())
                .date(plan.getDate())
                .section(plan.getSection())
                .content(plan.getContent())
                .build();
    }
}