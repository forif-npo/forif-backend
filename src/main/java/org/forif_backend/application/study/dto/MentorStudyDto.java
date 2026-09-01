package org.forif_backend.application.study.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.MentorStudy;

@Getter
@RequiredArgsConstructor
@Builder
public class MentorStudyDto {
    private final Long mentorId;
    private final String mentorName;
    private final Integer mentorNum;

    public static MentorStudyDto from(MentorStudy mentorStudy) {
        return MentorStudyDto.builder()
                .mentorId(mentorStudy.getMentor().getId())
                .mentorName(mentorStudy.getMentor().getUserName())
                .mentorNum(mentorStudy.getMentorNum())
                .build();
    }
}