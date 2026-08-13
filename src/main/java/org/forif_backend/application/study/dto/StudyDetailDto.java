package org.forif_backend.application.study.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
@Builder
public class StudyDetailDto {
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
    private final String locationDetail;
    private final StudyDifficulty difficulty;
    private final String imgUrl;
    private final String thumbnailImage;
    private final Boolean isOnline;
    private final String goal;
    private final String selectionCriteria;
    private final Integer capacity;
    private final Boolean requiresInterview;
    private final LocalDateTime interviewDate;
    private final List<StudyPlanDto> plans;
    private final List<StudyReferenceDto> references;
    private final List<MentorStudyDto> mentors;

    public static StudyDetailDto of(Study study, List<StudyPlan> plans,
                                     List<StudyReference> references,
                                     List<MentorStudy> mentorStudies) {
        return of(study, plans, references, mentorStudies, study.getThumbnailImage());
    }

    public static StudyDetailDto of(Study study, List<StudyPlan> plans,
                                     List<StudyReference> references,
                                     List<MentorStudy> mentorStudies,
                                     String thumbnailImage) {
        return of(study, plans, references, mentorStudies, thumbnailImage, Map.of());
    }

    public static StudyDetailDto of(Study study, List<StudyPlan> plans,
                                     List<StudyReference> references,
                                     List<MentorStudy> mentorStudies,
                                     String thumbnailImage,
                                     Map<UUID, String> referenceContents) {
        return StudyDetailDto.builder()
                .id(study.getId())
                .actYear(study.getActYear())
                .actSemester(study.getActSemester())
                .studyName(study.getStudyName())
                .primaryMentorName(study.getPrimaryMentorName())
                .secondaryMentorName(study.getSecondaryMentorName())
                .tags(study.getTags().stream().map(StudyTagDto::from).toList())
                .recruitStatus(study.getRecruitStatus())
                .oneLiner(study.getOneLiner())
                .explanation(study.getExplanation())
                .startTime(study.getStartTime())
                .endTime(study.getEndTime())
                .weekDay(study.getWeekDay())
                .location(study.getLocation())
                .locationDetail(study.getLocationDetail())
                .difficulty(study.getDifficulty())
                .imgUrl(study.getImgUrl())
                .thumbnailImage(thumbnailImage)
                .isOnline(study.getIsOnline())
                .goal(study.getGoal())
                .selectionCriteria(study.getSelectionCriteria())
                .capacity(study.getCapacity())
                .requiresInterview(study.getRequiresInterview())
                .interviewDate(study.getInterviewDate())
                .plans(plans.stream().map(StudyPlanDto::from).toList())
                .references(references.stream()
                        .map(reference -> StudyReferenceDto.from(
                                reference,
                                referenceContents.getOrDefault(reference.getId(), reference.getContent())
                        ))
                        .toList())
                .mentors(mentorStudies.stream().map(MentorStudyDto::from).toList())
                .build();
    }
}
