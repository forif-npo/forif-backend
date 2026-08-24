package org.forif_backend.application.study.dto;

import lombok.Builder;
import lombok.Getter;
import org.forif_backend.domain.study.ReferenceType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 스터디 부분 수정 입력. null인 필드는 변경하지 않는다.
 *
 * <p>secondaryMentorIdPresent는 "부멘토를 비운다"(null 전달)와 "건드리지 않는다"(필드 생략)를
 * 구분하기 위한 값으로, 요청 본문에 키가 있었는지를 나타낸다.
 */
@Getter
@Builder
public class UpdateStudyCommand {

    private final String studyName;
    private final String oneLiner;
    private final String explanation;
    private final String goal;
    private final String startTime;
    private final String endTime;
    private final Integer weekDay;
    private final String location;
    private final String locationDetail;
    private final Boolean isOnline;
    private final Integer difficulty;
    private final Integer capacity;
    private final String selectionCriteria;
    private final Boolean requiresInterview;
    private final LocalDateTime interviewDate;
    private final List<Long> studyTagIds;
    private final List<String> studyTagNames;
    private final Long secondaryMentorId;
    private final boolean secondaryMentorIdPresent;
    private final List<CreateStudyApplyCommand.Plan> studyPlanList;
    private final List<Reference> references;
    private final List<UUID> retainedReferenceIds;

    @Getter
    @Builder
    public static class Reference {
        private final ReferenceType type;
        private final String url;
        private final String fileName;
    }
}
