package org.forif_backend.application.study.dto;

import lombok.Builder;
import lombok.Getter;
import org.forif_backend.domain.study.ReferenceType;
import org.forif_backend.domain.study.StudyApplyData;

import java.time.LocalDateTime;
import java.util.List;

/** 스터디 개설 신청·재신청 입력. */
@Getter
@Builder
public class CreateStudyApplyCommand {

    private final String title;
    private final String oneLiner;
    private final List<Long> studyTagId;
    private final List<String> studyTagNames;
    private final String explanation;
    private final Boolean isOnline;
    private final String studyLocation;
    private final String studyLocationDetail;
    private final Integer weekDay;
    private final String startTime;
    private final String endTime;
    private final List<Plan> studyPlanList;
    private final Integer difficulty;
    private final String selectionCriteria;
    private final Integer capacity;
    private final Boolean requiresInterview;
    private final LocalDateTime interviewDate;
    private final List<Reference> references;
    private final Long secondaryMentorId;

    /** 요청에 부멘토 필드가 실려 있었는지. 생략(변경 없음)과 null 전달(제거)을 구분한다. */
    private final boolean secondaryMentorIdPresent;

    /** 엔티티에 반영할 값만 추린다. */
    public StudyApplyData toApplyData() {
        return StudyApplyData.builder()
                .title(title)
                .oneLiner(oneLiner)
                .explanation(explanation)
                .isOnline(isOnline)
                .studyLocation(studyLocation)
                .studyLocationDetail(studyLocationDetail)
                .weekDay(weekDay)
                .startTime(startTime)
                .endTime(endTime)
                .difficulty(difficulty)
                .selectionCriteria(selectionCriteria)
                .capacity(capacity)
                .requiresInterview(requiresInterview)
                .interviewDate(interviewDate)
                .build();
    }

    @Getter
    @Builder
    public static class Plan {
        private final Integer weekNum;
        private final LocalDateTime date;
        private final String topic;
        private final String content;
    }

    @Getter
    @Builder
    public static class Reference {
        private final ReferenceType type;
        private final String url;
        private final String fileName;
    }
}
