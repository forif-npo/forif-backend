package org.forif_backend.web.study;

import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.study.dto.StudyDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.forif_backend.common.dto.request.PageRequest;
import org.forif_backend.application.study.StudyService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.domain.study.StudyDifficulty;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.web.study.dto.StudiesResponse;

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
        List<String> tagList = tags != null ? Arrays.asList(tags) : null;

        // Search studies for condition
        List<StudyDto> studies = studyService.getStudies(
                pageRequest.getPage(), pageRequest.getPageSize(), year, semester, difficulties, tagList, recruitStatus, search);

        // Convert Study service DTOs to api response DTOs
        StudiesResponse response = StudiesResponse.from(studies);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
