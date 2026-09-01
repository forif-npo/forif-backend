package org.forif_backend.application.study.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyDifficulty;

import java.util.List;

@Getter
@RequiredArgsConstructor
@Builder
public class StudyDto {
    private final Integer id;
    private final int actYear;
    private final int actSemester;
    private final String studyName;
    private final String primaryMentorName;
    private final String secondaryMentorName;
    private final List<StudyTagDto> tags;
    private final RecruitStatus recruitStatus;
    private final String oneLiner;
    private final String explanation;
    private final String startTime;
    private final String endTime;
    private final Integer weekDay;
    private final String location;
    private final StudyDifficulty difficulty;
    private final String imgUrl;
    private final String thumbnailImage;
    private final boolean autonomousStudy;

    public static StudyDto from(Study studyEntity) {
        return from(studyEntity, null);
    }

    public static StudyDto from(Study studyEntity, String thumbnailImage) {
        return StudyDto.builder()
                .id(studyEntity.getId())
                .actYear(studyEntity.getActYear())
                .actSemester(studyEntity.getActSemester())
                .studyName(studyEntity.getStudyName())
                .primaryMentorName(studyEntity.getPrimaryMentorName())
                .secondaryMentorName(studyEntity.getSecondaryMentorName())
                .tags(studyEntity.getTags().stream().map(tag -> StudyTagDto.from(tag)).toList())
                .recruitStatus(studyEntity.getRecruitStatus())
                .oneLiner(studyEntity.getOneLiner())
                .explanation(studyEntity.getExplanation())
                .startTime(studyEntity.getStartTime())
                .endTime(studyEntity.getEndTime())
                .weekDay(studyEntity.getWeekDay())
                .location(studyEntity.getLocation())
                .difficulty(studyEntity.getDifficulty())
                .imgUrl(studyEntity.getImgUrl())
                .thumbnailImage(thumbnailImage)
                .autonomousStudy(studyEntity.isAutonomousStudy())
                .build();
    }
}
