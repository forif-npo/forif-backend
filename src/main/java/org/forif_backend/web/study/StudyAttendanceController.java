package org.forif_backend.web.study;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.study.StudyAttendanceService;
import org.forif_backend.application.study.dto.AttendanceCommand;
import org.forif_backend.application.study.dto.StudyAttendanceResult;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.study.dto.StudyAttendanceResponse;
import org.forif_backend.web.study.dto.UpdateAttendanceRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "스터디 출석", description = "멘토의 스터디 출석 관리 API")
@RestController
@RequiredArgsConstructor
public class StudyAttendanceController {

    private final StudyAttendanceService studyAttendanceService;

    @Operation(summary = "스터디 출석 현황 조회 (멘토 전용)",
            description = "해당 스터디 멘티 전원의 주차별 출석 기록을 조회합니다.")
    @GetMapping("/api/v1/studies/{studyId}/attendances")
    public ResponseEntity<ApiResponse<StudyAttendanceResponse>> getAttendance(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "스터디 ID") @PathVariable Integer studyId
    ) {
        StudyAttendanceResult result = studyAttendanceService.getAttendance(userId, studyId);
        return ResponseEntity.ok(ApiResponse.success(StudyAttendanceResponse.from(result)));
    }

    @Operation(summary = "스터디 출석 기록 저장 (멘토 전용)",
            description = "멘티들의 주차별 출석 상태를 일괄 저장합니다. 기존 기록이 있으면 갱신됩니다.")
    @PutMapping("/api/v1/studies/{studyId}/attendances")
    public ResponseEntity<ApiResponse<Void>> updateAttendance(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "스터디 ID") @PathVariable Integer studyId,
            @Valid @RequestBody UpdateAttendanceRequest request
    ) {
        List<AttendanceCommand> commands = request.attendances().stream()
                .map(item -> new AttendanceCommand(
                        item.userId(), item.weekNum(), item.status(), item.studyDate()))
                .toList();

        studyAttendanceService.updateAttendance(userId, studyId, commands);
        return ResponseEntity.ok(ApiResponse.successWithMsg("Success"));
    }
}
