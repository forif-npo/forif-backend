package org.forif_backend.web.studyApply;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.studyApply.dto.CreateStudyApplyInfo;
import org.forif_backend.application.studyApply.StudyApplyService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.studyApply.dto.CreateStudyApplyRequest;
import org.forif_backend.web.studyApply.dto.CreateStudyApplyResponse;
import org.forif_backend.web.studyApply.mapper.StudyApplyMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/study-apply")
public class StudyApplyController {
    private final StudyApplyService studyApplyService;

    @PostMapping("")
    public ResponseEntity<ApiResponse<CreateStudyApplyResponse>> applyStudy(@AuthenticationPrincipal Long userId,
                                                                            @RequestPart(name = "createStudyApplyRequest") @Valid CreateStudyApplyRequest createStudyApplyRequest,
                                                                            @RequestPart(name = "thumbnail", required = false) MultipartFile thumbnail,
                                                                            @RequestPart(name = "references", required = false) List<MultipartFile> references
                                                    ) {
        CreateStudyApplyInfo info = studyApplyService.createStudyApply(userId, createStudyApplyRequest, thumbnail, references);
        return ResponseEntity.ok().body(ApiResponse.success(StudyApplyMapper.from(info)));
    }
}
