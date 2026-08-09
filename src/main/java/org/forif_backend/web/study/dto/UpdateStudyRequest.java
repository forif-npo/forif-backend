package org.forif_backend.web.study.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.forif_backend.domain.study.ReferenceType;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<String> studyTagNames;
    private List<Plan> studyPlanList;
    private List<Reference> references;

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
    }
}
