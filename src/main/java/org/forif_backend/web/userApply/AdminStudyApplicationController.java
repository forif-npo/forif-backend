package org.forif_backend.web.userApply;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.user.UserApplyService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.web.userApply.dto.AcceptRequest;
import org.forif_backend.web.userApply.dto.AdminStudyApplicationResponse;
import org.forif_backend.web.userApply.dto.RejectRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/** 현재 학기 스터디 신청을 운영진이 조회하는 API. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/study-applications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStudyApplicationController {

    private final UserApplyService userApplyService;
    private final SemesterService semesterService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminStudyApplicationResponse>>> getApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(value = "sort", required = false) List<String> sort
    ) {
        SemesterInfo active = semesterService.getActive();
        return ResponseEntity.ok(ApiResponse.success(AdminStudyApplicationResponse.fromPage(
                userApplyService.getAdminApplications(active.actYear(), active.actSemester(), page, size, search,
                        SortCriteria.parse(sort, Set.of("userId", "userName", "department", "studyName", "priority", "appliedAt")))
        )));
    }

    @PostMapping("/{studyId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptAutonomousApplications(
            @PathVariable Integer studyId,
            @jakarta.validation.Valid @RequestBody AcceptRequest request
    ) {
        userApplyService.acceptAutonomousStudyApplications(studyId, request.applyIds());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{studyId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectAutonomousApplications(
            @PathVariable Integer studyId,
            @jakarta.validation.Valid @RequestBody RejectRequest request
    ) {
        userApplyService.rejectAutonomousStudyApplications(studyId, request.applyIds());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
