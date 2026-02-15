package org.forif_backend.application.staff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.staff.dto.CreateMentorCommand;
import org.forif_backend.application.staff.dto.StaffSignInCommand;
import org.forif_backend.application.staff.dto.StaffSignInResult;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.dto.response.CursorPageResponse;
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

import java.util.List;

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
     * 멘토 계정 생성 (운영진 전용)
     */
    @Transactional
    public void createMentorAccount(CreateMentorCommand command) {
        if (staffAccountRepository.existsById(command.userId())) {
            throw new ForifException(ErrorCode.STAFF_ALREADY_EXISTS);
        }

        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        String encodedPassword = passwordEncoder.encode(command.password());

        StaffAccount staffAccount = StaffAccount.createStaffAccount(
                user,
                encodedPassword,
                user.getUserName(),
                StaffRole.MENTOR,
                command.affiliation()
        );

        staffAccountRepository.save(staffAccount);
    }

    /**
     * 멘토 정보 수정 (운영진 전용)
     */
    @Transactional
    public void updateMentorAccount(Long userId, String name, String password, String affiliation) {
        StaffAccount staffAccount = staffAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        String encodedPassword = password != null ? passwordEncoder.encode(password) : null;
        staffAccount.updateInfo(name, encodedPassword, affiliation);
    }

    /**
     * 멘토 계정 삭제 (운영진 전용)
     */
    @Transactional
    public void deleteMentorAccount(Long userId) {
        if (!staffAccountRepository.existsById(userId)) {
            throw new ForifException(ErrorCode.STAFF_NOT_FOUND);
        }

        staffAccountRepository.deleteById(userId);
    }

    /**
     * 멘토 목록 조회 (운영진 전용, 커서 페이지네이션)
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<StaffAccount> getMentors(Long cursor, int size, String search) {
        List<StaffAccount> staffAccounts = staffAccountRepository.searchWithCursor(cursor, size, search);
        long totalElements = staffAccountRepository.count(search);

        boolean hasNext = staffAccounts.size() > size;
        List<StaffAccount> content = hasNext ? staffAccounts.subList(0, size) : staffAccounts;

        Integer nextCursor = hasNext ? content.get(content.size() - 1).getUserId().intValue() : null;

        return new CursorPageResponse<>(content, nextCursor, hasNext, totalElements);
    }

    /**
     * 현재 로그인한 스태프 정보 조회
     */
    @Transactional(readOnly = true)
    public StaffAccount getStaffInfo(Long userId) {
        return staffAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));
    }
}
