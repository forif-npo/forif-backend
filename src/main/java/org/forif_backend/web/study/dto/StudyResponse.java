package org.forif_backend.web.study.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StudyResponse {
    private Integer id;
    private String studyName;
    private String primaryMentorName;
    private String secondaryMentorName;
    private List<String> tags;
    private String recruitStatus;
    private String oneLiner;
    private String explanation;
    private String startTime;
    private String endTime;
    private Integer weekDay;
    private String location;
    private String difficulty;
    private String imgUrl;
    private Integer actYear;
    private Integer actSemester;
}
