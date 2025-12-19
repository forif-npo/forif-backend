package org.forif_backend.web.studyApply.dto;

import jakarta.validation.constraints.*;
import org.forif_backend.domain.studyApply.ReferenceType;
import org.hibernate.validator.constraints.Length;

import java.time.ZonedDateTime;
import java.util.List;

public record CreateStudyApplyRequest(
        @NotBlank
        String title,                   // 스터디 이름
        @NotBlank
        String subTitle,                // 스터디 한 줄 설명
        @Size(min = 1, message = "스터디 태그는 최소 1개 이상 선택해야 합니다.")
        List<Long> studyTagId,          // 스터디 태그 id
        @NotBlank
        @Length(min = 50, max = 500, message = "스터디 목표는 50자 이상 500자 이내로 작성해주세요.")
        String goal,                    // 스터디 목표
        @NotBlank
        @Length(min = 50, max = 500, message = "스터디 소개는 50자 이상 500자 이내로 작성해주세요.")
        String explanation,             // 스터디 소개
        @NotNull
        Boolean isOnline,               // 온라인 진행 여부
        @NotNull
        String studyLocation,           // 진행 장소
        @NotBlank
        String studyLocationDetail,     // 강의실(호)
        @NotNull
        Integer weekDay,                // 진행 요일
        @NotBlank
        @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "시간 형식은 HH:MM으로 입력해야 합니다.")
        String startTime,               // 시작 시간
        @NotBlank
        @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "시간 형식은 HH:MM으로 입력해야 합니다.")
        String endTime,                 // 종료 시간
        List<Plan> studyPlanList,       // 주차별 계획
        @NotNull
        Integer difficulty,             // 난이도
        @NotBlank
        @Length(max = 100, message = "선정기준은 100자 이내로 작성해주세요.")
        String selectionCriteria,       // 선정 기준
        @NotNull
        @Min(value = 1, message = "모집 인원은 최소 1명 이상이어야 합니다.")
        Integer capacity,               // 모집 인원
        @NotNull
        Boolean requiresInterview,      // 면접 여부
        ZonedDateTime interviewDate,    // 면접 날짜
        List<Reference> references      // 참고자료
) {
    public record Reference(
            ReferenceType type,         // 유형
            String url,                 // 유형이 url일 경우 url 문자열
            String fileName             // 유형이 파일일 경우 첨부한 파일의 originalName
    ) {}

    public record Plan(
            Integer weekNum,            // 주차
            ZonedDateTime date,         // 날짜
            String topic,               // 주제
            String content              // 내용
    ) {}
}
