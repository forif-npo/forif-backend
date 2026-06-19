package org.forif_backend.application.study.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@RequiredArgsConstructor
@Builder
public class AdminStudyDto {
    private final Integer id;
    private final String studyName;
    private final String primaryMentorName;
    private final String secondaryMentorName;
    private final List<StudyTagDto> tags;
    private final String oneLiner;
    private final long menteeCount;
    private final RecruitStatus recruitStatus;
    private final StudyStatus studyStatus;
    private final String rejectReason;
    private final LocalDateTime createdAt;

    public static AdminStudyDto of(Study study, long menteeCount) {
        return AdminStudyDto.builder()
                .id(study.getId())
                .studyName(study.getStudyName())
                .primaryMentorName(study.getPrimaryMentorName())
                .secondaryMentorName(study.getSecondaryMentorName())
                .tags(study.getTags().stream().map(StudyTagDto::from).toList())
                .oneLiner(study.getOneLiner())
                .menteeCount(menteeCount)
                .recruitStatus(study.getRecruitStatus())
                .studyStatus(study.getStudyStatus())
                .rejectReason(study.getRejectReason())
                .createdAt(study.getCreatedAt())
                .build();
    }
}
