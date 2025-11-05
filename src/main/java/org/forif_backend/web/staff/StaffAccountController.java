package org.forif_backend.web.staff;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.staff.dto.StaffSignInCommand;
import org.forif_backend.application.staff.dto.StaffSignInResult;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.web.staff.dto.StaffSignInRequest;
import org.forif_backend.web.staff.dto.StaffSignInResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff")
public class StaffAccountController {

    private final StaffAccountService staffAccountService;

    /**
     * 스태프(멘토/운영진) 로그인
     */
    @PostMapping("/signin")
    public ResponseEntity<ApiResponse<StaffSignInResponse>> staffSignIn(@RequestBody StaffSignInRequest request) {
        // 1. Web DTO → Application Command 변환
        StaffSignInCommand command = StaffDtoMapper.toCommand(request);

        // 2. Service 호출
        StaffSignInResult result = staffAccountService.staffSignIn(command);

        // 3. Application Result → Web DTO 변환
        StaffSignInResponse response = StaffDtoMapper.toResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}