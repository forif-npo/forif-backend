package org.forif_backend.application.staff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.dto.response.ApiResponse;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.web.staff.dto.StaffSignInRequest;
import org.forif_backend.web.staff.dto.StaffSignInResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffAccountService {
    
    private final StaffAccountRepository staffAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    
    /**
     * 스태프(멘토/운영진) 로그인
     */
    public ApiResponse<StaffSignInResponse> staffSignIn(StaffSignInRequest request) {
        StaffAccount staffAccount = staffAccountRepository.findByUserId(request.userId())
            .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND, "등록되지 않은 스태프입니다."));

        if (!passwordEncoder.matches(request.password(), staffAccount.getPassword())) {
            throw new ForifException(ErrorCode.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtProvider.generateAccessToken(staffAccount.getUser().getId().toString());
        String refreshToken = jwtProvider.generateRefreshToken(staffAccount.getUser().getId().toString());

        StaffSignInResponse response = StaffSignInResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .staffRole(staffAccount.getRole().getValue()) // "MENTOR" or "ADMIN"
            .userId(staffAccount.getUser().getId())
            .staffName(staffAccount.getName())
            .build();
            
        return ApiResponse.success(response);
    }
}
