package org.forif_backend.web.study.dto;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;

import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyTag;

@Getter
@Builder
public class StudyResponse {
    private Integer id;
    private String studyName;
    private String primaryMentorName;
    private String secondaryMentorName;
    private List<String> tags;
    private String recruitStatus;
    private String oneLiner;
    private String explanation;
    private String startTime;
    private String endTime;
    private Integer weekDay;
    private String location;
    private String difficulty;
    private String imgUrl;
    private Integer actYear;
    private Integer actSemester;

    public static StudyResponse from(Study study) {
        List<String> tagNames = study.getTags().stream()
                .map(StudyTag::getName)
                .collect(Collectors.toList());

        String recruitStatusValue = study.getRecruitStatus() != null
                ? study.getRecruitStatus().getValue()
                : null;

        return StudyResponse.builder()
                .id(study.getId())
                .studyName(study.getStudyName())
                .primaryMentorName(study.getPrimaryMentorName())
                .secondaryMentorName(study.getSecondaryMentorName())
                .tags(tagNames)
                .recruitStatus(recruitStatusValue)
                .oneLiner(study.getOneLiner())
                .explanation(study.getExplanation())
                .startTime(study.getStartTime())
                .endTime(study.getEndTime())
                .weekDay(study.getWeekDay())
                .location(study.getLocation())
                .difficulty(study.getDifficulty().getValue())
                .imgUrl(study.getImgUrl())
                .actYear(study.getActYear())
                .actSemester(study.getActSemester())
                .build();
    }
}
