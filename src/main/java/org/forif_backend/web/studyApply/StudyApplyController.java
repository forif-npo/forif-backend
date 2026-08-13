package org.forif_backend.web.studyApply;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.study.StudyService;
import org.forif_backend.application.study.dto.CreateStudyApplyInfo;
import org.forif_backend.application.study.dto.StudyApplicationDetailDto;
import org.forif_backend.application.study.dto.StudyApplicationDto;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.study.dto.CreateStudyApplyRequest;
import org.forif_backend.web.study.dto.CreateStudyApplyResponse;
import org.forif_backend.web.study.dto.StudyApplicationDetailResponse;
import org.forif_backend.web.study.dto.StudyApplicationResponse;
import org.forif_backend.web.study.dto.UpdateStudyRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "스터디 개설 신청", description = "멘토의 스터디 개설 신청 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/study-apply")
@Validated
public class StudyApplyController {

    private final StudyService studyService;
    private final ObjectMapper objectMapper;

    /**
     * 스터디 개설 신청
     * @param userId 사용자 ID
     * @param requestJson 스터디 개설 신청 정보 (JSON 문자열)
     * @param thumbnail 썸네일 이미지
     * @param references 참고 자료 파일들
     * @return 생성된 스터디 개설 신청 응답
     */
    @Operation(summary = "스터디 개설 신청", description = """
            스터디 개설을 신청합니다. multipart/form-data 형식으로 전송합니다.

            **파트 구성**
            - `studyRequest`: 스터디 신청 정보 (JSON)
            - `thumbnail`: 썸네일 이미지 파일 (선택)
            - `references`: 참고 자료 파일 목록 (선택)

            승인 대기(PENDING) 상태로 생성되며, 업로드된 파일의 조회 URL이 응답에 포함됩니다.
            """)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CreateStudyApplyResponse>> applyStudy(
            @AuthenticationPrincipal Long userId,
            @RequestPart("studyRequest") @Valid CreateStudyApplyRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "references", required = false) List<MultipartFile> references
    ) {
        CreateStudyApplyInfo info = studyService.createStudyApply(userId, request, thumbnail, references);
        return ResponseEntity.ok().body(ApiResponse.success(CreateStudyApplyResponse.from(info)));
    }

    @Operation(summary = "내 스터디 개설 신청 취소", description = "개설 신청 기간 내 승인 전 본인 신청서를 취소하고 삭제합니다.")
    @DeleteMapping("/{studyId}")
    public ResponseEntity<ApiResponse<Void>> cancelStudyApplication(
            @PathVariable Integer studyId,
            @AuthenticationPrincipal Long userId
    ) {
        studyService.cancelStudyApplication(studyId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "내 스터디 개설 신청 목록 조회", description = "승인 전 또는 반려된 본인의 스터디 개설 신청서를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<StudyApplicationResponse>>> getMyStudyApplications(
            @AuthenticationPrincipal Long userId
    ) {
        List<StudyApplicationResponse> applications = studyService.getMyStudyApplications(userId)
                .stream()
                .map(StudyApplicationResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    @Operation(summary = "내 스터디 개설 신청 상세 조회", description = "본인이 개설한 승인 전 또는 반려된 신청서만 조회할 수 있습니다.")
    @GetMapping("/{studyId}")
    public ResponseEntity<ApiResponse<StudyApplicationDetailResponse>> getMyStudyApplication(
            @PathVariable Integer studyId,
            @AuthenticationPrincipal Long userId
    ) {
        StudyApplicationDetailDto application = studyService.getMyStudyApplication(userId, studyId);
        return ResponseEntity.ok(ApiResponse.success(StudyApplicationDetailResponse.from(
                application.getStudy(),
                application.getStudyStatus(),
                application.getRejectReason(),
                application.isCanModify()
        )));
    }

    @Operation(summary = "내 스터디 개설 신청 수정", description = "승인 전 본인 신청서의 변경한 항목만 수정할 수 있으며, 반려 건은 수정 후 재신청 상태로 전환됩니다.")
    @PatchMapping(value = "/{studyId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CreateStudyApplyResponse>> updateStudyApplication(
            @PathVariable Integer studyId,
            @AuthenticationPrincipal Long userId,
            @RequestPart("studyRequest") @Valid UpdateStudyRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "references", required = false) List<MultipartFile> references
    ) {
        CreateStudyApplyInfo info = studyService.updateStudyApplication(studyId, userId, request, thumbnail, references);
        return ResponseEntity.ok(ApiResponse.success(CreateStudyApplyResponse.from(info)));
    }

    @Operation(summary = "스터디 개설 재신청", description = """
            거절된 스터디 개설 신청서를 수정하여 재신청합니다.

            거절(REJECTED) 상태인 신청서만 재신청이 가능하며, 재신청 후 RE_APPLIED 상태로 변경됩니다.
            """)
    @PatchMapping(value = "/{studyId}/re-apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CreateStudyApplyResponse>> reApplyStudy(
            @Parameter(description = "재신청할 스터디 ID") @PathVariable Integer studyId,
            @AuthenticationPrincipal Long userId,
            @RequestPart("studyRequest") @Valid CreateStudyApplyRequest request, // 여기서 자동으로 ObjectMapper가 작동합니다!
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "references", required = false) List<MultipartFile> references) {

        CreateStudyApplyInfo info = studyService.reApplyStudy(studyId, userId, request, thumbnail, references);
        return ResponseEntity.ok().body(ApiResponse.success(CreateStudyApplyResponse.from(info)));
    }
}
