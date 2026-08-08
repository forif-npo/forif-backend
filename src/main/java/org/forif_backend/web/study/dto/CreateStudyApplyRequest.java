package org.forif_backend.web.study.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.forif_backend.domain.study.ReferenceType;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateStudyApplyRequest {

    /**
     * record 사용 시 title 만 파싱 되고 나머지 필드들이 모두 null값이 들어와서 일반 class로 수정하였습니다.
     * 이에 따른 모든 accessor 메서드를 getter로 변경하였습니다.
     */

    @NotBlank
    private String title;                   // 스터디 이름

    @NotBlank
    @Length(max = 100, message = "한 줄 소개는 100자 이내로 작성해주세요.")
    private String oneLiner;                // 스터디 한 줄 소개

    @Size(min = 1, message = "스터디 태그는 최소 1개 이상 선택해야 합니다.")
    private List<Long> studyTagId;          // 스터디 태그 id

    @NotBlank
    @Length(min = 50, max = 500, message = "스터디 목표는 50자 이상 500자 이내로 작성해주세요.")
    private String goal;                    // 스터디 목표

    @NotBlank
    @Length(min = 50, max = 500, message = "스터디 소개는 50자 이상 500자 이내로 작성해주세요.")
    private String explanation;             // 스터디 소개

    @NotNull
    private Boolean isOnline;               // 온라인 진행 여부

    @NotNull
    private String studyLocation;           // 진행 장소

    private String studyLocationDetail;     // 강의실(호)

    @NotNull
    private Integer weekDay;                // 진행 요일

    @NotBlank
    @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "시간 형식은 HH:MM으로 입력해야 합니다.")
    private String startTime;               // 시작 시간

    @NotBlank
    @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "시간 형식은 HH:MM으로 입력해야 합니다.")
    private String endTime;                 // 종료 시간

    private List<Plan> studyPlanList;       // 주차별 계획

    @NotNull
    private Integer difficulty;             // 난이도

    @NotBlank
    @Length(max = 100, message = "선정기준은 100자 이내로 작성해주세요.")
    private String selectionCriteria;       // 선정 기준

    @NotNull
    @Min(value = 1, message = "모집 인원은 최소 1명 이상이어야 합니다.")
    private Integer capacity;               // 모집 인원

    @NotNull
    private Boolean requiresInterview;      // 면접 여부

    private LocalDateTime interviewDate;    // 면접 날짜

    private List<Reference> references;     // 참고자료

    private Long secondaryMentorId;         // 부멘토 유저 ID

    @AssertTrue(message = "온라인 또는 장소 미정이 아닌 경우 강의실(호)을 입력해야 합니다.")
    public boolean isStudyLocationDetailValid() {
        return Boolean.TRUE.equals(isOnline)
                || "장소 미정".equals(studyLocation)
                || (studyLocationDetail != null && !studyLocationDetail.isBlank());
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Reference {
        private ReferenceType type;         // 유형
        private String url;                 // 유형이 url일 경우 url 문자열
        private String fileName;            // 유형이 파일일 경우 첨부한 파일의 originalName
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Plan {
        private Integer weekNum;            // 주차
        private LocalDateTime date;         // 날짜
        private String topic;               // 주제
        private String content;             // 내용
    }
}
