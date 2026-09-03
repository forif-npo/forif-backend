package org.forif_backend.application.staff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.staff.dto.CreateAdminCommand;
import org.forif_backend.application.staff.dto.MentorHistory;
import org.forif_backend.application.staff.dto.MentorSummary;
import org.forif_backend.application.staff.dto.StaffSignInCommand;
import org.forif_backend.application.staff.dto.StaffSignInResult;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.common.util.PasswordUtils;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.file.TransactionalFileCleanup;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffAccountService {

    private static final String FILE_CLEANUP_CONTEXT = "회장 서명";

    private final SemesterService semesterService;
    private final StaffAccountRepository staffAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final ForifTeamRepository forifTeamRepository;
    private final FilePort filePort;

    /** 운영진 스태프 로그인 */
    public StaffSignInResult staffSignIn(StaffSignInCommand command) {
        StaffAccount staffAccount = staffAccountRepository.findByUserIdAndRole(command.userId(), StaffRole.ADMIN)
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

    /** 현재 로그인한 운영진의 비밀번호를 변경하고 기존 ADMIN 세션을 무효화한다. */
    @Transactional
    public void changeAdminPassword(Long userId, String currentPassword, String newPassword) {
        StaffAccount staffAccount = staffAccountRepository.findByUserIdAndRole(userId, StaffRole.ADMIN)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, staffAccount.getPassword())) {
            throw new ForifException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (passwordEncoder.matches(newPassword, staffAccount.getPassword())) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
        PasswordUtils.validate(newPassword);

        staffAccount.updatePassword(passwordEncoder.encode(newPassword));
        refreshTokenService.deleteRefreshToken(userId.toString(), StaffRole.ADMIN.getValue());
    }

    /**
     * 멘토 전체 목록 조회 (운영진 전용, 커서 페이지네이션)
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<MentorSummary> getMentors(Long cursor, Integer page, int size, String search) {
        return getMentors(cursor, page, size, search, List.of());
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<MentorSummary> getMentors(Long cursor, Integer page, int size, String search, List<SortCriteria> sorting) {
        long totalElements = studyRepository.countMentors(search);

        return toMentorPage(CursorPageResponse.paginate(
                page, size, totalElements,
                () -> studyRepository.searchMentorsWithOffset(page, size, search, sorting),
                () -> studyRepository.searchMentors(cursor, size, search),
                user -> user.getId().intValue()), null, null);
    }

    /**
     * 학기별 멘토 목록 조회 (운영진 전용, 커서/오프셋 페이지네이션)
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<MentorSummary> getMentors(int year, int semester, Long cursor, Integer page, int size, String search, List<SortCriteria> sorting) {
        long totalElements = studyRepository.countMentorsByYearSemester(year, semester, search);

        return toMentorPage(CursorPageResponse.paginate(
                page, size, totalElements,
                () -> studyRepository.searchMentorsByYearSemesterWithOffset(year, semester, page, size, search, sorting),
                () -> studyRepository.searchMentorsByYearSemester(year, semester, cursor, size, search),
                user -> user.getId().intValue()), year, semester);
    }

    private CursorPageResponse<MentorSummary> toMentorPage(
            CursorPageResponse<User> page,
            Integer year,
            Integer semester
    ) {
        List<Long> userIds = page.content().stream().map(User::getId).toList();
        Map<Long, String> studyNames = studyRepository.findMentorStudyNamesByUserIds(userIds, year, semester);
        return page.withContent(page.content().stream()
                .map(user -> MentorSummary.from(
                        user,
                        studyNames.get(user.getId())
                ))
                .toList());
    }

    /** 부원 이력 상세에서 사용할 멘토 활동 이력 조회. */
    @Transactional(readOnly = true)
    public List<MentorHistory> getMentorHistory(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));
        return studyRepository.findMentorHistoryByMentorId(userId).stream()
                .map(MentorHistory::from)
                .toList();
    }

    /**
     * 현재 로그인한 스태프 정보 조회
     */
    @Transactional(readOnly = true)
    public StaffAccount getStaffInfo(Long userId) {
        return staffAccountRepository.findByUserIdAndRole(userId, StaffRole.ADMIN)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));
    }

    // ==================== 회장단 운영진 관리 ====================

    /**
     * 회장단(회장/부회장) 권한 검증 — 다른 도메인에서도 사용한다.
     */
    public void requirePresidentTeam(Long userId) {
        validatePresidentTeam(userId);
    }

    /** 회장단(회장/부회장) 여부를 확인한다. 리소스 소유권 권한과 조합할 때 사용한다. */
    @Transactional(readOnly = true)
    public boolean isPresidentTeam(Long userId) {
        StaffAccount staffAccount = staffAccountRepository.findByUserIdAndRole(userId, StaffRole.ADMIN)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        String affiliation = staffAccount.getAffiliation();
        return "회장".equals(affiliation) || "부회장".equals(affiliation);
    }

    /**
     * 회장 권한 검증 — 다른 도메인에서도 사용한다.
     * 회장직 인수인계가 걸린 작업(학기 전환 등)은 부회장이 할 수 없다.
     */
    public void requirePresident(Long userId) {
        validatePresident(userId);
    }

    /**
     * 회장단(회장/부회장) 권한 검증
     */
    private StaffAccount validatePresidentTeam(Long userId) {
        StaffAccount staffAccount = staffAccountRepository.findByUserIdAndRole(userId, StaffRole.ADMIN)
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
        StaffAccount staffAccount = staffAccountRepository.findByUserIdAndRole(userId, StaffRole.ADMIN)
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

        return CursorPageResponse.paginate(
                page, size, totalElements,
                () -> staffAccountRepository.searchAdminsWithOffset(page, size, search),
                () -> staffAccountRepository.searchAdminsWithCursor(cursor, size, search),
                account -> account.getUserId().intValue());
    }

    /**
     * 운영진 계정 생성
     */
    @Transactional
    public StaffAccount createAdmin(Long requesterId, CreateAdminCommand command) {
        validatePresidentTeam(requesterId);

        if (staffAccountRepository.existsByUserIdAndRole(command.userId(), StaffRole.ADMIN)) {
            throw new ForifException(ErrorCode.STAFF_ALREADY_EXISTS);
        }
        PasswordUtils.validate(command.password());

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
        SemesterInfo active = semesterService.getActive();
        int currentYear = active.actYear();
        int currentSemester = active.actSemester();
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
     *
     * 회장 보호는 여기 두지 않는다. 이 헬퍼는 수정도 함께 쓰기 때문에, 여기서 막으면
     * 회장 비밀번호 재설정까지 불가능해진다. 요청자는 자기 자신을 관리할 수 없어
     * 회장 본인도 못 바꾸고, 부회장은 권한이 없어 아무도 손댈 수 없게 된다.
     */
    private StaffAccount findAndValidateTargetAdmin(StaffAccount requester, Long targetUserId) {
        if (requester.getUserId().equals(targetUserId)) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        StaffAccount target = staffAccountRepository.findByUserIdAndRole(targetUserId, StaffRole.ADMIN)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

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
            PasswordUtils.validate(password);
            staffAccount.updatePassword(passwordEncoder.encode(password));
            // 비밀번호 재설정은 대개 유출 대응이다. 셀프 변경처럼 기존 세션을 끊지 않으면
            // 탈취범의 refresh 토큰이 30일 동안 계속 로테이션되며 살아남는다.
            refreshTokenService.deleteRefreshToken(targetUserId.toString(), StaffRole.ADMIN.getValue());
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
     * 운영진 계정 삭제.
     * 회장은 삭제할 수 없다. 지우려면 먼저 차기 회장에게 위임해야 한다.
     */
    @Transactional
    public void deleteAdmin(Long requesterId, Long targetUserId) {
        StaffAccount requester = validatePresidentTeam(requesterId);
        StaffAccount target = findAndValidateTargetAdmin(requester, targetUserId);

        if ("회장".equals(target.getAffiliation())) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        String signatureObjectKey = target.getSignatureObjectKey();
        staffAccountRepository.delete(target);
        refreshTokenService.deleteRefreshToken(targetUserId.toString(), StaffRole.ADMIN.getValue());
        // 계정이 사라지면 서명 이미지를 참조하는 곳이 없어진다
        TransactionalFileCleanup.deleteAfterCommit(filePort, signatureObjectKey, FILE_CLEANUP_CONTEXT);
    }

    /**
     * 학기 전환 시 차기 회장 인수인계.
     * 대상은 ADMIN 계정을 가진 기존 운영진이어야 하며, 현 회장 본인이면(연임) 아무것도 바꾸지 않는다.
     */
    @Transactional
    public void handOverPresidency(Long currentPresidentUserId, Long nextPresidentUserId) {
        StaffAccount president = validatePresident(currentPresidentUserId);

        if (president.getUserId().equals(nextPresidentUserId)) {
            return; // 연임
        }

        StaffAccount next = staffAccountRepository.findByUserIdAndRole(nextPresidentUserId, StaffRole.ADMIN)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        next.updateAffiliation("회장");
        president.updateAffiliation("운영진");
    }

    /** 회장 후보 = ADMIN 계정을 가진 운영진 */
    @Transactional(readOnly = true)
    public boolean isAdminAccount(Long userId) {
        return staffAccountRepository.existsByUserIdAndRole(userId, StaffRole.ADMIN);
    }

    /**
     * 회장/부회장 위임
     */
    @Transactional
    public void delegate(Long requesterId, Long targetUserId, String targetAffiliation) {
        StaffAccount president = validatePresident(requesterId);

        // 회장 연임(자기 자신 지정)은 허용한다. 부회장 자리에 본인을 넣는 것은 막는다.
        if (president.getUserId().equals(targetUserId) && !"회장".equals(targetAffiliation)) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }

        StaffAccount target = staffAccountRepository.findByUserIdAndRole(targetUserId, StaffRole.ADMIN)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

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
