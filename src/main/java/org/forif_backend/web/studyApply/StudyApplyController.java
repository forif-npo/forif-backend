package org.forif_backend.web.studyApply;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.study.StudyService;
import org.forif_backend.application.study.dto.CreateStudyApplyInfo;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.study.dto.CreateStudyApplyRequest;
import org.forif_backend.web.study.dto.CreateStudyApplyResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @PatchMapping(value = "/{studyId}/re-apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CreateStudyApplyResponse> reApplyStudy(
            @PathVariable Integer studyId,
            @AuthenticationPrincipal Long userId,
            @RequestPart("studyRequest") @Valid CreateStudyApplyRequest request, // 여기서 자동으로 ObjectMapper가 작동합니다!
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "referenceFiles", required = false) List<MultipartFile> referenceFiles) {

        CreateStudyApplyInfo info = studyService.reApplyStudy(studyId, userId, request, thumbnail, referenceFiles);
        return ApiResponse.success(CreateStudyApplyResponse.from(info));
    }
}
