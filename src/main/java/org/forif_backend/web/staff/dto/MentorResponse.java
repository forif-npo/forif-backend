package org.forif_backend.web.staff.dto;

import lombok.Builder;
import org.forif_backend.application.staff.dto.MentorSummary;

@Builder
public record MentorResponse(
    Long userId,
    String name,
    String department,
    String phoneNum,
    String studyName
) {
    public static MentorResponse from(MentorSummary mentor) {
        return MentorResponse.builder()
                .userId(mentor.userId())
                .name(mentor.name())
                .department(mentor.department())
                .phoneNum(mentor.phoneNum())
                .studyName(mentor.studyName())
                .build();
    }
}
