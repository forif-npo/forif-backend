package org.forif_backend.domain.study;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 개설 신청서가 스터디에 반영하는 값 묶음.
 * 최초 신청과 재신청이 같은 형태를 쓴다.
 *
 * <p>난이도는 저장 형태(1~5 level)로 받아 엔티티가 enum으로 바꾼다.
 */
@Builder
public record StudyApplyData(
        String title,
        String oneLiner,
        String explanation,
        Boolean isOnline,
        String studyLocation,
        String studyLocationDetail,
        Integer weekDay,
        String startTime,
        String endTime,
        Integer difficulty,
        String selectionCriteria,
        Integer capacity,
        Boolean requiresInterview,
        LocalDateTime interviewDate
) {
}
