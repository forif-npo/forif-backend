package org.forif_backend.web.study;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.forif_backend.common.dto.request.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.forif_backend.application.study.StudyService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.domain.study.StudyDifficulty;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyTag;
import org.forif_backend.web.study.dto.StudiesResponse;
import org.forif_backend.web.study.dto.StudyResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/studies")
public class StudyController {

    private final StudyService studyService;

    @GetMapping
    public ResponseEntity<ApiResponse<StudiesResponse>> getStudies(
            @ModelAttribute PageRequest pageRequest,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) List<StudyDifficulty> difficulties,
            @RequestParam(required = false) String[] tags,
            @RequestParam(value = "recruit_status", required = false) RecruitStatus recruitStatus,
            @RequestParam(required = false) String search) {

        // Convert params
        List<String> tagList = getTagList(tags);

        // Search studies for condition
        List<Study> studies = studyService.getStudies(
                pageRequest.getPage(), pageRequest.getPageSize(), year, semester, difficulties, tagList, recruitStatus, search);

        // Convert Study entities to response DTOs
        StudiesResponse response = buildResponse(studies);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private List<String> getTagList(String[] tags) {
        List<String> tagList = null;
        if (tags != null && tags.length > 0) {
            tagList = Arrays.asList(tags);
        }
        return tagList;
    }

    private StudiesResponse buildResponse(List<Study> studies) {
        List<StudyResponse> studyResponses = studies.stream()
                .map(this::convertToStudyResponse)
                .collect(Collectors.toList());

        StudiesResponse response = StudiesResponse.builder()
                .studies(studyResponses)
                .build();
        return response;
    }

    private RecruitStatus convertRecruitStatusAsEnum(String recruitStatus) {
        RecruitStatus recruitStatusEnum = null;
        if (recruitStatus != null) {
            recruitStatusEnum = RecruitStatus.fromValue(recruitStatus);
        }
        return recruitStatusEnum;
    }

    private List<StudyDifficulty> convertDifficultiesAsEnum(String[] difficulties) {
        List<StudyDifficulty> difficultyEnums = null;
        if (difficulties != null && difficulties.length > 0) {
            difficultyEnums = Arrays.stream(difficulties)
                    .map(StudyDifficulty::fromValue)
                    .collect(Collectors.toList());
        }
        return difficultyEnums;
    }

    private StudyResponse convertToStudyResponse(Study study) {
        List<String> tagNames = study.getTags().stream()
                .map(StudyTag::getName)
                .collect(Collectors.toList());

        String recruitStatusValue = study.getRecruitStatus() != null
                ? study.getRecruitStatus().getValue()
                : null;

        return StudyResponse.builder()
                .id(study.getId())
                .studyName(study.getStudyName())
                .primaryMentorName(study.getPrimaryMentorName())
                .secondaryMentorName(study.getSecondaryMentorName())
                .tags(tagNames)
                .recruitStatus(recruitStatusValue)
                .oneLiner(study.getOneLiner())
                .explanation(study.getExplanation())
                .startTime(study.getStartTime())
                .endTime(study.getEndTime())
                .weekDay(study.getWeekDay())
                .location(study.getLocation())
                .difficulty(study.getDifficulty().getValue())
                .imgUrl(study.getImgUrl())
                .actYear(study.getActYear())
                .actSemester(study.getActSemester())
                .build();
    }
}
