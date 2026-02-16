package org.forif_backend.application.staff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.staff.dto.CreateAdminCommand;
import org.forif_backend.application.staff.dto.StaffSignInCommand;
import org.forif_backend.application.staff.dto.StaffSignInResult;
import org.forif_backend.application.staff.dto.StaffSignUpCommand;
import org.forif_backend.application.staff.dto.StaffSignUpResult;
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


    /**
     * 현재 로그인한 스태프 정보 조회
     */
    @Transactional(readOnly = true)
    public StaffAccount getStaffInfo(Long userId) {
        return staffAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));
    }

    // ==================== 회장단 운영진 관리 ====================

    /**
     * 회장단(회장/부회장) 권한 검증
     */
    private StaffAccount validatePresidentTeam(Long userId) {
        StaffAccount staffAccount = staffAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        String affiliation = staffAccount.getAffiliation();
        if (!"회장".equals(affiliation) && !"부회장".equals(affiliation)) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        return staffAccount;
    }

    /**
     * 회장 전용 권한 검증
     */
    private StaffAccount validatePresident(Long userId) {
        StaffAccount staffAccount = staffAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        if (!"회장".equals(staffAccount.getAffiliation())) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        return staffAccount;
    }

    /**
     * 운영진 목록 조회 (커서 페이지네이션)
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<StaffAccount> getAdmins(Long requesterId, Long cursor, int size, String search) {
        validatePresidentTeam(requesterId);

        List<StaffAccount> staffAccounts = staffAccountRepository.searchAdminsWithCursor(cursor, size, search);
        long totalElements = staffAccountRepository.countAdmins(search);

        boolean hasNext = staffAccounts.size() > size;
        List<StaffAccount> content = hasNext ? staffAccounts.subList(0, size) : staffAccounts;

        Integer nextCursor = hasNext ? content.get(content.size() - 1).getUserId().intValue() : null;

        return new CursorPageResponse<>(content, nextCursor, hasNext, totalElements);
    }

    /**
     * 운영진 계정 생성
     */
    @Transactional
    public StaffAccount createAdmin(Long requesterId, CreateAdminCommand command) {
        validatePresidentTeam(requesterId);

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
                StaffRole.ADMIN,
                command.affiliation()
        );

        return staffAccountRepository.save(staffAccount);
    }

    /**
     * 대상 운영진 조회 + 권한 검증 (공통)
     * - ADMIN role 확인
     * - 부회장 대상은 회장만 관리 가능
     * - 자기 자신은 관리 불가
     */
    private StaffAccount findAndValidateTargetAdmin(StaffAccount requester, Long targetUserId) {
        if (requester.getUserId().equals(targetUserId)) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        StaffAccount target = staffAccountRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        if (target.getRole() != StaffRole.ADMIN) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        if ("부회장".equals(target.getAffiliation()) && !"회장".equals(requester.getAffiliation())) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        return target;
    }

    /**
     * 운영진 정보 수정
     */
    @Transactional
    public StaffAccount updateAdmin(Long requesterId, Long targetUserId, String name, String password, String affiliation) {
        StaffAccount requester = validatePresidentTeam(requesterId);
        StaffAccount staffAccount = findAndValidateTargetAdmin(requester, targetUserId);

        if (name != null) {
            staffAccount.updateName(name);
        }
        if (password != null) {
            staffAccount.updatePassword(passwordEncoder.encode(password));
        }
        if (affiliation != null) {
            staffAccount.updateAffiliation(affiliation);
        }

        return staffAccount;
    }

    /**
     * 운영진 계정 삭제
     */
    @Transactional
    public void deleteAdmin(Long requesterId, Long targetUserId) {
        StaffAccount requester = validatePresidentTeam(requesterId);
        findAndValidateTargetAdmin(requester, targetUserId);

        staffAccountRepository.deleteById(targetUserId);
    }

    /**
     * 회장/부회장 위임
     */
    @Transactional
    public void delegate(Long requesterId, Long targetUserId, String targetAffiliation) {
        StaffAccount president = validatePresident(requesterId);

        if (president.getUserId().equals(targetUserId)) {
            throw new ForifException(ErrorCode.BAD_REQUEST);
        }

        StaffAccount target = staffAccountRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        if (target.getRole() != StaffRole.ADMIN) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        if ("회장".equals(targetAffiliation)) {
            target.updateAffiliation("회장");
            president.updateAffiliation("운영진");
        } else if ("부회장".equals(targetAffiliation)) {
            List<StaffAccount> currentVPs = staffAccountRepository.findByAffiliation("부회장");
            for (StaffAccount vp : currentVPs) {
                vp.updateAffiliation("운영진");
            }
            target.updateAffiliation("부회장");
        } else {
            throw new ForifException(ErrorCode.BAD_REQUEST);
        }
    }
}
