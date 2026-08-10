package org.forif_backend.web.study;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.study.StudyService;
import org.forif_backend.application.study.dto.AdminStudyDto;
import org.forif_backend.application.study.dto.StudyDetailDto;
import org.forif_backend.application.study.dto.StudyDto;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.StudyDifficulty;
import org.forif_backend.domain.study.StudyStatus;
import org.forif_backend.web.study.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Tag(name = "스터디", description = "스터디 조회 및 어드민 관리 API")
@RestController
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    /**
     * [유저용] 스터디 목록 조회 (cursor/offset pagination)
     */
    @Operation(summary = "스터디 목록 조회", description = """
            cursor 또는 page 중 하나를 사용하세요.
            - cursor: 커서 기반 페이지네이션 (무한 스크롤). next_cursor 값을 다음 요청의 cursor로 전달.
            - page: 오프셋 기반 페이지네이션 (0부터 시작). cursor와 함께 사용 불가.
            둘 다 생략 시 cursor 모드로 첫 페이지를 반환합니다.
            연도/학기/난이도/태그/모집상태/검색어로 필터링할 수 있습니다.
            """)
    @GetMapping("/api/v1/studies")
    public ResponseEntity<ApiResponse<CursorPageResponse<StudyResponse>>> getStudies(
            @Parameter(description = "이전 페이지의 마지막 스터디 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드, cursor와 함께 사용 불가)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "조회 연도 (예: 2025)") @RequestParam(required = false) Integer year,
            @Parameter(description = "조회 학기 (1 또는 2)") @RequestParam(required = false) Integer semester,
            @Parameter(description = "난이도 필터 (BEGINNER, INTERMEDIATE, ADVANCED)") @RequestParam(required = false) List<StudyDifficulty> difficulties,
            @Parameter(description = "태그 이름 필터 (복수 입력 가능)") @RequestParam(required = false) String[] tags,
            @Parameter(description = "모집 상태 필터 (OPEN=모집중, CLOSED=모집마감)") @RequestParam(value = "recruit_status", required = false) RecruitStatus recruitStatus,
            @Parameter(description = "스터디 이름 검색어") @RequestParam(required = false) String search) {

        List<String> tagList = tags != null ? Arrays.asList(tags) : null;

        CursorPageResponse<StudyDto> result = studyService.getStudies(
                cursor, page, size, year, semester, difficulties, tagList, recruitStatus, search);

        List<StudyResponse> content = result.content().stream()
                .map(StudyResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(result.withContent(content)));
    }

    /**
     * [어드민용] 스터디 목록 조회 (cursor/offset pagination)
     */
    @Operation(summary = "스터디 목록 조회 (어드민 전용)", description = """
            cursor 또는 page 중 하나를 사용하세요.
            - cursor: 커서 기반 페이지네이션 (무한 스크롤). next_cursor 값을 다음 요청의 cursor로 전달.
            - page: 오프셋 기반 페이지네이션 (0부터 시작). cursor와 함께 사용 불가.
            둘 다 생략 시 cursor 모드로 첫 페이지를 반환합니다.
            """)
    @GetMapping("/api/v1/admin/studies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminStudyResponse>>> getAdminStudies(
            @Parameter(description = "이전 페이지의 마지막 스터디 ID (cursor 모드)") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "페이지 번호, 0부터 시작 (offset 모드, cursor와 함께 사용 불가)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 당 항목 수") @RequestParam int size,
            @Parameter(description = "조회 연도") @RequestParam(required = false) Integer year,
            @Parameter(description = "조회 학기 (1 또는 2)") @RequestParam(required = false) Integer semester,
            @Parameter(description = "스터디 이름 검색어") @RequestParam(required = false) String search,
            @Parameter(description = "스터디 승인 상태 필터") @RequestParam(value = "study_status", required = false) List<StudyStatus> studyStatuses
    ) {
        CursorPageResponse<AdminStudyDto> result = studyService.getAdminStudies(cursor, page, size, year, semester, search, studyStatuses);

        List<AdminStudyResponse> content = result.content().stream()
                .map(AdminStudyResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(result.withContent(content)));
    }

    /**
     * 스터디 상세 조회
     */
    @Operation(summary = "스터디 상세 조회", description = "스터디의 상세 정보(커리큘럼, 멘토, 참고자료 등)를 조회합니다.")
    @GetMapping("/api/v1/studies/{studyId}")
    public ResponseEntity<ApiResponse<StudyDetailResponse>> getStudyDetail(
            @Parameter(description = "스터디 ID") @PathVariable Integer studyId) {
        StudyDetailDto studyDetail = studyService.getStudyDetail(studyId);
        return ResponseEntity.ok(ApiResponse.success(StudyDetailResponse.from(studyDetail)));
    }

    /**
     * [어드민용] 스터디 수정
     */
    @Operation(summary = "스터디 정보 수정 (어드민 전용)", description = "스터디의 이름, 설명, 태그, 커리큘럼 등을 수정합니다.")
    @PatchMapping("/api/v1/admin/studies/{studyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateStudy(
            @Parameter(description = "수정할 스터디 ID") @PathVariable Integer studyId,
            @RequestBody @Valid UpdateStudyRequest request
    ) {
        studyService.updateStudy(studyId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * [어드민용] 스터디 삭제
     */
    @Operation(summary = "스터디 삭제 (어드민 전용)", description = "스터디와 관련된 플랜, 참고자료, 수강자 정보를 모두 삭제합니다.")
    @DeleteMapping("/api/v1/admin/studies/{studyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStudy(
            @Parameter(description = "삭제할 스터디 ID") @PathVariable Integer studyId
    ) {
        studyService.deleteStudy(studyId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 멘토가 개설한 스터디 조회
     * @param userId 멘토 ID (인증된 사용자)
     * @return 멘토가 개설한 스터디 리스트
     */
    @Operation(summary = "내가 개설한 스터디 조회 (멘토 전용)", description = "로그인한 멘토가 개설한 스터디 목록을 조회합니다.")
    @GetMapping("/api/v1/studies/me/created")
    public ResponseEntity<ApiResponse<List<StudyResponse>>> getMyCreatedStudies(@AuthenticationPrincipal Long userId)
    {
        List<StudyDto> studies = studyService.getMyCreatedStudies(userId);

        return ResponseEntity.ok(ApiResponse.success(StudyResponse.fromList(studies)));
    }

    /**
     * [어드민용] 스터디 승인
     */
    @Operation(summary = "스터디 개설 승인 (어드민 전용)", description = "스터디 개설 신청을 승인합니다. 승인 시 멘토 계정이 자동으로 생성됩니다.")
    @PatchMapping("/api/v1/admin/studies/{studyId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveStudy(
            @Parameter(description = "승인할 스터디 ID") @PathVariable Integer studyId) {
        studyService.approveStudy(studyId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * [어드민용] 스터디 거절
     */
    @Operation(summary = "스터디 개설 거절 (어드민 전용)", description = "스터디 개설 신청을 거절합니다. 거절 사유를 반드시 입력해야 합니다.")
    @PatchMapping("/api/v1/admin/studies/{studyId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectStudy(
            @Parameter(description = "거절할 스터디 ID") @PathVariable Integer studyId,
            @RequestBody @Valid StudyRejectRequest request // String reason을 담은 DTO
    ) {
        studyService.rejectStudy(studyId, request.reason());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
