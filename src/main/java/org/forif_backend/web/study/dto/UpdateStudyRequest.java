package org.forif_backend.web.study.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import org.forif_backend.domain.study.ReferenceType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UpdateStudyRequest {

    private String studyName;
    private String oneLiner;
    private String explanation;
    private String goal;

    private String startTime;
    private String endTime;
    private Integer weekDay;

    private String location;
    private String locationDetail;
    private Boolean isOnline;

    private Integer difficulty;
    private Integer capacity;
    private String selectionCriteria;
    private Boolean requiresInterview;
    private LocalDateTime interviewDate;

    private List<Long> studyTagIds;
    private List<@NotBlank String> studyTagNames;
    private List<Plan> studyPlanList;
    private List<Reference> references;
    private List<UUID> retainedReferenceIds;

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
