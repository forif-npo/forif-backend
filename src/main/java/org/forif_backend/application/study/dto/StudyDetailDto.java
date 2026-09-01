package org.forif_backend.application.study.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final boolean autonomousStudy;
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
                .autonomousStudy(study.isAutonomousStudy())
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
                .mentors(resolveMentors(study, mentorStudies))
                .build();
    }

    /**
     * 멘토 목록.
     *
     * <p>FOR-116에서 멘토가 레거시 조인 테이블(tb_mentor_study)에서 tb_study의 FK 컬럼으로
     * 옮겨졌다. 그 이후 개설된 스터디는 조인 테이블에 행이 없으므로, 비어 있으면 FK 컬럼에서
     * 만들어 준다. 그러지 않으면 클라이언트는 부멘토가 없는 것으로 오해하고,
     * 그 상태로 저장하면 실제 부멘토가 지워진다.
     *
     * <p>이름은 비정규화 컬럼을 쓰므로 지연 로딩된 멘토 연관을 초기화하지 않는다.
     */
    private static List<MentorStudyDto> resolveMentors(Study study, List<MentorStudy> mentorStudies) {
        if (mentorStudies != null && !mentorStudies.isEmpty()) {
            return mentorStudies.stream().map(MentorStudyDto::from).toList();
        }

        List<MentorStudyDto> mentors = new ArrayList<>();
        if (study.getPrimaryMentor() != null) {
            mentors.add(MentorStudyDto.builder()
                    .mentorId(study.getPrimaryMentor().getId())
                    .mentorName(study.getPrimaryMentorName())
                    .mentorNum(1)
                    .build());
        }
        if (study.getSecondaryMentor() != null) {
            mentors.add(MentorStudyDto.builder()
                    .mentorId(study.getSecondaryMentor().getId())
                    .mentorName(study.getSecondaryMentorName())
                    .mentorNum(2)
                    .build());
        }
        return mentors;
    }
}
