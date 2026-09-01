package org.forif_backend.application.study.dto;

import lombok.Builder;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyTag;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public record StudyInfo(
        Integer studyId,
        String studyName,
        String primaryMentorName,
        String secondaryMentorName,
        List<String> tags,
        String oneLiner,
        String startTime,
        String endTime,
        Integer weekDay,
        String location,
        Integer difficulty,
        String imgUrl,
        String thumbnailImage,
        boolean certificateIssued
) {
    /**
     * Study 엔티티를 StudyInfo DTO로 변환
     */
    public static StudyInfo from(Study study) {
        return from(study, false);
    }

    public static StudyInfo from(Study study, boolean certificateIssued) {
        return from(study, certificateIssued, null);
    }

    public static StudyInfo from(Study study, boolean certificateIssued, String thumbnailImage) {
        // tags 리스트를 태그 이름 리스트로 변환
        List<String> tagNames = study.getTags() != null
                ? study.getTags().stream()
                        .map(StudyTag::getName)
                        .collect(Collectors.toList())
                : List.of();

        return StudyInfo.builder()
                .studyId(study.getId())
                .studyName(study.getStudyName())
                .primaryMentorName(study.getPrimaryMentorName())
                .secondaryMentorName(study.getSecondaryMentorName())
                .tags(tagNames)
                .oneLiner(study.getOneLiner())
                .startTime(study.getStartTime())
                .endTime(study.getEndTime())
                .weekDay(study.getWeekDay())
                .location(study.getLocation())
                .difficulty(study.getDifficulty() != null ? study.getDifficulty().getLevel() : null)
                .imgUrl(study.getImgUrl())
                .thumbnailImage(thumbnailImage)
                .certificateIssued(certificateIssued)
                .build();
    }
}
