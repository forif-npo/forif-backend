package org.forif_backend.web.department;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.department.DepartmentService;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.department.dto.DepartmentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "학과", description = "단과대학·학과 기준 데이터 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    @Operation(summary = "학과 목록 조회", description = "회원가입과 프로필 수정에 사용할 단과대학·학과 목록을 조회합니다. 인증 없이 접근 가능합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getDepartments() {
        return ResponseEntity.ok(ApiResponse.success(
                departmentService.getAll().stream().map(DepartmentResponse::from).toList()));
    }
}
