package org.forif_backend.web.semester;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.semester.dto.SemesterResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "학기", description = "동아리 활동 학기 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/semesters")
public class SemesterController {

    private final SemesterService semesterService;

    @Operation(summary = "현재 활동 학기 조회",
            description = "운영진이 지정한 현재 활동 학기를 조회합니다. 인증 없이 접근 가능합니다.")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SemesterResponse>> getCurrentSemester() {
        SemesterInfo active = semesterService.getActive();
        return ResponseEntity.ok(ApiResponse.success(SemesterResponse.from(active)));
    }

    @Operation(summary = "선택 가능한 학기 목록",
            description = "동아리 창립 학기부터 다음 학기까지의 목록을 최신순으로 조회합니다. 인증 없이 접근 가능합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SemesterResponse>>> getSemesters() {
        List<SemesterInfo> semesters = semesterService.getSelectableSemesters();
        return ResponseEntity.ok(ApiResponse.success(SemesterResponse.fromList(semesters)));
    }
}
