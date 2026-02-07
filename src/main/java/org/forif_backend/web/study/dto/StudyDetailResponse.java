package org.forif_backend.web.study.dto;

import org.forif_backend.application.study.dto.MentorStudyDto;
import org.forif_backend.application.study.dto.StudyDetailDto;
import org.forif_backend.application.study.dto.StudyPlanDto;
import org.forif_backend.application.study.dto.StudyReferenceDto;
import org.forif_backend.application.study.dto.StudyTagDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StudyDetailResponse(
        Integer id,
        int actYear,
        int actSemester,
        String studyName,
        String subTitle,
        String primaryMentorName,
        String secondaryMentorName,
        List<String> tags,
        String recruitStatus,
        String oneLiner,
        String explanation,
        String startTime,
        String endTime,
        Integer weekDay,
        String location,
        String locationDetail,
        String difficulty,
        String imgUrl,
        String thumbnailImage,
        Boolean isOnline,
        String goal,
        String selectionCriteria,
        Integer capacity,
        Boolean requiresInterview,
        List<PlanResponse> plans,
        List<ReferenceResponse> references,
        List<MentorResponse> mentors
) {
    public record PlanResponse(
            Long id,
            int weekNum,
            LocalDateTime date,
            String section,
            String content
    ) {
        public static PlanResponse from(StudyPlanDto dto) {
            return new PlanResponse(dto.getId(), dto.getWeekNum(), dto.getDate(), dto.getSection(), dto.getContent());
        }
    }

    public record ReferenceResponse(
            UUID id,
            String referenceType,
            String content
    ) {
        public static ReferenceResponse from(StudyReferenceDto dto) {
            return new ReferenceResponse(dto.getId(), dto.getReferenceType().name(), dto.getContent());
        }
    }

    public record MentorResponse(
            Long mentorId,
            String mentorName,
            Integer mentorNum
    ) {
        public static MentorResponse from(MentorStudyDto dto) {
            return new MentorResponse(dto.getMentorId(), dto.getMentorName(), dto.getMentorNum());
        }
    }

    public static StudyDetailResponse from(StudyDetailDto dto) {
        List<String> tagNames = dto.getTags().stream()
                .map(StudyTagDto::getName)
                .toList();

        String recruitStatusValue = dto.getRecruitStatus() != null
                ? dto.getRecruitStatus().getValue()
                : null;

        String difficultyValue = dto.getDifficulty() != null
                ? dto.getDifficulty().getValue()
                : null;

        return new StudyDetailResponse(
                dto.getId(),
                dto.getActYear(),
                dto.getActSemester(),
                dto.getStudyName(),
                dto.getSubTitle(),
                dto.getPrimaryMentorName(),
                dto.getSecondaryMentorName(),
                tagNames,
                recruitStatusValue,
                dto.getOneLiner(),
                dto.getExplanation(),
                dto.getStartTime(),
                dto.getEndTime(),
                dto.getWeekDay(),
                dto.getLocation(),
                dto.getLocationDetail(),
                difficultyValue,
                dto.getImgUrl(),
                dto.getThumbnailImage(),
                dto.getIsOnline(),
                dto.getGoal(),
                dto.getSelectionCriteria(),
                dto.getCapacity(),
                dto.getRequiresInterview(),
                dto.getPlans().stream().map(PlanResponse::from).toList(),
                dto.getReferences().stream().map(ReferenceResponse::from).toList(),
                dto.getMentors().stream().map(MentorResponse::from).toList()
        );
    }
}
