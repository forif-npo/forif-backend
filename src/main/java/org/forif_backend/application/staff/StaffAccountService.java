package org.forif_backend.application.staff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.staff.dto.StaffSignInCommand;
import org.forif_backend.application.staff.dto.StaffSignInResult;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
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
    public StaffSignInResult staffSignIn(StaffSignInCommand command) {
        StaffAccount staffAccount = staffAccountRepository.findByUserId(command.userId())
            .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        if (!passwordEncoder.matches(command.password(), staffAccount.getPassword())) {
            throw new ForifException(ErrorCode.PASSWORD_MISMATCH);
        }

        String role = staffAccount.getRole().getValue();
        String accessToken = jwtProvider.generateAccessToken(staffAccount.getUserId().toString(), role);
        String refreshToken = jwtProvider.generateRefreshToken(staffAccount.getUserId().toString());

        return new StaffSignInResult(
            accessToken,
            refreshToken,
            role
        );
    }
}
