package org.forif_backend.web.study.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import org.forif_backend.domain.study.ReferenceType;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UpdateStudyRequest {

    @JsonAlias("title")
    private String studyName;
    private String oneLiner;
    @Length(max = 3000, message = "스터디 소개는 최대 3000자 이내로 작성해주세요.")
    private String explanation;

    @Length(max = 3000, message = "스터디 목표는 최대 3000자 이내로 작성해주세요.")
    private String goal;

    private String startTime;
    private String endTime;
    private Integer weekDay;

    @JsonAlias("study_location")
    private String location;
    @JsonAlias("study_location_detail")
    private String locationDetail;
    private Boolean isOnline;

    private Integer difficulty;
    private Integer capacity;
    private String selectionCriteria;
    private Boolean requiresInterview;
    private LocalDateTime interviewDate;

    @JsonAlias("study_tag_id")
    private List<Long> studyTagIds;
    private List<@NotBlank String> studyTagNames;
    private Long secondaryMentorId;
    @JsonIgnore
    private boolean secondaryMentorIdPresent;
    private List<Plan> studyPlanList;
    private List<Reference> references;
    private List<UUID> retainedReferenceIds;

    @JsonSetter("secondary_mentor_id")
    public void setSecondaryMentorId(Long secondaryMentorId) {
        this.secondaryMentorId = secondaryMentorId;
        this.secondaryMentorIdPresent = true;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Plan {
        private Integer weekNum;
        private LocalDateTime date;
        private String topic;
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Reference {
        private ReferenceType type;
        private String url;
        private String fileName;
    }
}
