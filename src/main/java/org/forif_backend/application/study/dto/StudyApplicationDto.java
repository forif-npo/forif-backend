package org.forif_backend.application.study.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyStatus;
import org.forif_backend.domain.study.StudyTag;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@RequiredArgsConstructor
@Builder
public class StudyApplicationDto {
    private final Integer id;
    private final String studyName;
    private final String oneLiner;
    private final List<String> tags;
    private final StudyStatus studyStatus;
    private final String rejectReason;
    private final LocalDateTime createdAt;
    private final boolean canModify;

    public static StudyApplicationDto from(Study study, boolean canModify) {
        return StudyApplicationDto.builder()
                .id(study.getId())
                .studyName(study.getStudyName())
                .oneLiner(study.getOneLiner())
                .tags(study.getTags().stream().map(StudyTag::getName).toList())
                .studyStatus(study.getStudyStatus())
                .rejectReason(study.getRejectReason())
                .createdAt(study.getCreatedAt())
                .canModify(canModify)
                .build();
    }
}
