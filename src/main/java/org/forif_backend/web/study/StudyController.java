package org.forif_backend.web.study;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.study.StudyService;
import org.forif_backend.application.study.dto.StudyDto;
import org.forif_backend.common.dto.request.PageRequest;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.StudyDifficulty;
import org.forif_backend.web.study.dto.StudyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    /**
     * [유저용] 스터디 목록 조회 (offset pagination)
     */
    @GetMapping("/api/v1/studies")
    public ResponseEntity<ApiResponse<List<StudyResponse>>> getStudies(
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


        return ResponseEntity.ok(ApiResponse.success(StudyResponse.fromList(studies)));
    }

    /**
     * [어드민용] 스터디 목록 조회 (cursor pagination)
     */
    @GetMapping("/api/v1/admin/studies")
    public ResponseEntity<ApiResponse<CursorPageResponse<StudyResponse>>> getAdminStudies(
            @RequestParam(required = false) Integer cursor,
            @RequestParam int size,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) String search
    ) {
        CursorPageResponse<StudyDto> result = studyService.getAdminStudies(cursor, size, year, semester, search);

        List<StudyResponse> content = result.content().stream()
                .map(StudyResponse::from)
                .toList();

        CursorPageResponse<StudyResponse> response = new CursorPageResponse<>(
                content, result.nextCursor(), result.hasNext(), result.totalElements()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 멘토가 개설한 스터디 조회
     * @param userId 멘토 ID (인증된 사용자)
     * @return 멘토가 개설한 스터디 리스트
     */
    @GetMapping("/api/v1/studies/me/created")
    public ResponseEntity<ApiResponse<List<StudyResponse>>> getMyCreatedStudies(@AuthenticationPrincipal Long userId)
    {
        List<StudyDto> studies = studyService.getMyCreatedStudies(userId);

        return ResponseEntity.ok(ApiResponse.success(StudyResponse.fromList(studies)));
    }
}
