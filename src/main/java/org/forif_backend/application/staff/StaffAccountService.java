package org.forif_backend.application.staff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.staff.dto.StaffSignInCommand;
import org.forif_backend.application.staff.dto.StaffSignInResult;
import org.forif_backend.application.staff.dto.StaffSignUpCommand;
import org.forif_backend.application.staff.dto.StaffSignUpResult;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffAccountService {

    private final StaffAccountRepository staffAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

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
        String userId = staffAccount.getUserId().toString();
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        // Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(userId, refreshToken);

        return new StaffSignInResult(
                accessToken,
                refreshToken,
                role
        );
    }

    /**
     * 스태프(멘토/운영진) 회원가입
     */
    @Transactional
    public StaffSignUpResult staffSignUp(StaffSignUpCommand command) {
        // 이미 스태프 계정이 존재하는지 확인
        if (staffAccountRepository.existsById(command.userId())) {
            throw new ForifException(ErrorCode.STAFF_ALREADY_EXISTS);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(command.password());

        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        // 스태프 계정 생성
        StaffAccount staffAccount = StaffAccount.createStaffAccount(
                user,
                encodedPassword,
                command.name(),
                command.role(),
                command.affiliation()
        );

        staffAccountRepository.save(staffAccount);

        StaffRole role = staffAccount.getRole();
        String roleValue = role.getValue();
        String staffId = staffAccount.getUserId().toString();
        String accessToken = jwtProvider.generateAccessToken(staffId, roleValue);
        String refreshToken = jwtProvider.generateRefreshToken(staffId);

        // 5. Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(staffId, refreshToken);

        return new StaffSignUpResult(
                accessToken,
                refreshToken,
                role
        );
    }

}
