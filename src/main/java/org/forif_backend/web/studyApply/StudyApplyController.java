package org.forif_backend.web.studyApply;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.study.StudyService;
import org.forif_backend.application.study.dto.CreateStudyApplyInfo;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.study.dto.CreateStudyApplyRequest;
import org.forif_backend.web.study.dto.CreateStudyApplyResponse;
import org.forif_backend.web.study.mapper.StudyApplyMapper;
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

    /**
     * 스터디 개설 신청
     * @param userIdStr 사용자 ID (문자열)
     * @param requestJson 스터디 개설 신청 정보 (JSON 문자열)
     * @param thumbnail 썸네일 이미지
     * @param references 참고 자료 파일들
     * @return 생성된 스터디 개설 신청 응답
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CreateStudyApplyResponse>> applyStudy(
            @AuthenticationPrincipal String userIdStr,
            @RequestPart(name = "createStudyApplyRequest") String requestJson,
            @RequestPart(name = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(name = "references", required = false) List<MultipartFile> references
    ) throws Exception {


        // ObjectMapper 설정 (Java record 파싱을 위해)
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new ParameterNamesModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // JSON 문자열을 DTO로 파싱
        CreateStudyApplyRequest createStudyApplyRequest = mapper.readValue(requestJson, CreateStudyApplyRequest.class);

        // userId 변환
        Long userId = Long.parseLong(userIdStr);

        CreateStudyApplyInfo info = studyService.createStudyApply(userId, createStudyApplyRequest, thumbnail, references);
        return ResponseEntity.ok().body(ApiResponse.success(StudyApplyMapper.from(info)));
    }
}
