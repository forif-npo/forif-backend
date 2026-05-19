package org.forif_backend.application.staff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.staff.dto.CreateAdminCommand;
import org.forif_backend.application.staff.dto.CreateMentorCommand;
import org.forif_backend.application.staff.dto.StaffSignInCommand;
import org.forif_backend.application.staff.dto.StaffSignInResult;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.team.ForifTeamRepository;
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
    private final ForifTeamRepository forifTeamRepository;

    /**
     * 스태프(멘토/운영진) 로그인
     */
    public StaffSignInResult staffSignIn(StaffSignInCommand command) {
        StaffAccount staffAccount = staffAccountRepository.findByUserId(command.userId())
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        if (!passwordEncoder.matches(command.password(), staffAccount.getPassword())) {
            throw new ForifException(ErrorCode.PASSWORD_MISMATCH);
        }

        String affiliation = staffAccount.getAffiliation();
        String role = staffAccount.getRole().getValue();
        String userId = staffAccount.getUserId().toString();
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId, role);

        // Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(userId, role, refreshToken);

        return new StaffSignInResult(
                accessToken,
                refreshToken,
                role,
                affiliation
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

        if (staffAccount.getRole() != StaffRole.MENTOR) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }

        if (name != null) {
            staffAccount.getUser().updateUserName(name);
        }

        String encodedPassword = password != null ? passwordEncoder.encode(password) : null;
        staffAccount.updateInfo(name, encodedPassword, affiliation);
    }

    /**
     * 멘토 계정 삭제 (운영진 전용)
     */
    @Transactional
    public void deleteMentorAccount(Long userId) {
        StaffAccount staffAccount = staffAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        if (staffAccount.getRole() != StaffRole.MENTOR) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }

        staffAccountRepository.deleteById(staffAccount.getUserId());
    }

    /**
     * 멘토 전체 목록 조회 (운영진 전용, 커서 페이지네이션)
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<StaffAccount> getMentors(Long cursor, Integer page, int size, String search) {
        long totalElements = staffAccountRepository.count(search);

        if (page != null) {
            List<StaffAccount> staffAccounts = staffAccountRepository.searchMentorsWithOffset(page, size, search);
            boolean hasNext = (long) (page + 1) * size < totalElements;
            return CursorPageResponse.ofOffset(staffAccounts, hasNext, totalElements, page, size);
        }

        List<StaffAccount> staffAccounts = staffAccountRepository.searchWithCursor(cursor, size, search);
        boolean hasNext = staffAccounts.size() > size;
        List<StaffAccount> content = hasNext ? staffAccounts.subList(0, size) : staffAccounts;
        Integer nextCursor = hasNext ? content.get(content.size() - 1).getUserId().intValue() : null;
        return CursorPageResponse.ofCursor(content, nextCursor, hasNext, totalElements);
    }

    /**
     * 학기별 멘토 목록 조회 (운영진 전용, 커서/오프셋 페이지네이션)
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<StaffAccount> getMentors(int year, int semester, Long cursor, Integer page, int size, String search) {
        long totalElements = staffAccountRepository.countMentorsByYearSemester(year, semester, search);

        if (page != null) {
            List<StaffAccount> staffAccounts = staffAccountRepository.searchMentorsByYearSemesterWithOffset(year, semester, page, size, search);
            boolean hasNext = (long) (page + 1) * size < totalElements;
            return CursorPageResponse.ofOffset(staffAccounts, hasNext, totalElements, page, size);
        }

        List<StaffAccount> staffAccounts = staffAccountRepository.searchMentorsByYearSemester(year, semester, cursor, size, search);
        boolean hasNext = staffAccounts.size() > size;
        List<StaffAccount> content = hasNext ? staffAccounts.subList(0, size) : staffAccounts;
        Integer nextCursor = hasNext ? content.get(content.size() - 1).getUserId().intValue() : null;
        return CursorPageResponse.ofCursor(content, nextCursor, hasNext, totalElements);
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
    public CursorPageResponse<StaffAccount> getAdmins(Long requesterId, Integer cursor, Integer page, int size, String search) {
        validatePresidentTeam(requesterId);

        long totalElements = staffAccountRepository.countAdmins(search);

        if (page != null) {
            List<StaffAccount> staffAccounts = staffAccountRepository.searchAdminsWithOffset(page, size, search);
            boolean hasNext = (long) (page + 1) * size < totalElements;
            return CursorPageResponse.ofOffset(staffAccounts, hasNext, totalElements, page, size);
        }

        List<StaffAccount> staffAccounts = staffAccountRepository.searchAdminsWithCursor(cursor, size, search);
        boolean hasNext = staffAccounts.size() > size;
        List<StaffAccount> content = hasNext ? staffAccounts.subList(0, size) : staffAccounts;
        Integer nextCursor = hasNext ? content.get(content.size() - 1).getUserId().intValue() : null;
        return CursorPageResponse.ofCursor(content, nextCursor, hasNext, totalElements);
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
                "운영진"
        );

        StaffAccount saved = staffAccountRepository.save(staffAccount);

        // 운영진 이력 기록 (StaffAccount 삭제 후에도 남아야 함)
        int currentYear = DateUtils.getCurrentYear();
        int currentSemester = DateUtils.getCurrentSemester();
        if (!forifTeamRepository.existsByActYearAndActSemesterAndUserId(currentYear, currentSemester, user.getId())) {
            ForifTeam forifTeam = ForifTeam.create(user, currentYear, currentSemester, command.affiliation());
            forifTeamRepository.save(forifTeam);
        }

        return saved;
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
            if ("회장".equals(affiliation) || "부회장".equals(affiliation)
                    || "회장".equals(staffAccount.getAffiliation()) || "부회장".equals(staffAccount.getAffiliation())) {
                throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
            }
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
            throw new ForifException(ErrorCode.INVALID_INPUT);
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
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
    }
}
