package org.forif_backend.web.staff;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staff.StaffAccountService;
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
        ApiResponse<StaffSignInResponse> response = staffAccountService.staffSignIn(request);
        return ResponseEntity.ok(response);
    }
}