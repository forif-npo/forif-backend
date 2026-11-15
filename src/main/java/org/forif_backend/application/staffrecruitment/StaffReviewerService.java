package org.forif_backend.application.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.staffrecruitment.dto.StaffReviewerCandidateInfo;
import org.forif_backend.application.staffrecruitment.dto.StaffReviewerInfo;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.forif_backend.domain.staffrecruitment.StaffApplicationRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentReviewer;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentReviewerRepository;
import org.forif_backend.domain.staffrecruitment.StaffReviewerStatus;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 모집 회차별 지원서 열람 권한(reviewer) 관리와 검사.
 *
 * 전역 ADMIN 권한을 넓히지 않고 회차 단위 행으로만 열람을 허용한다.
 * 지원서 요청마다 이 서비스의 검사를 거치므로 회수·만료 후 다음 요청부터 바로 거절된다.
 */
@Service
@RequiredArgsConstructor
public class StaffReviewerService {

    private final StaffRecruitmentReviewerRepository staffRecruitmentReviewerRepository;
    private final StaffRecruitmentRepository staffRecruitmentRepository;
    private final StaffApplicationRepository staffApplicationRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final ForifTeamRepository forifTeamRepository;
    private final UserRepository userRepository;
    private final StaffAccountService staffAccountService;

    // ==================== 회장단 관리 ====================

    /**
     * 초대 후보 — 회차 학기의 운영진 명단.
     * ADMIN 계정이 없는 사람도 목록에는 보이되 부여는 거절한다. 화면에서 이유를 보여줄 수 있게 한다.
     */
    @Transactional(readOnly = true)
    public List<StaffReviewerCandidateInfo> getCandidates(Integer recruitmentId) {
        StaffRecruitment recruitment = requireRecruitment(recruitmentId);
        LocalDateTime now = LocalDateTime.now();

        List<ForifTeam> team = forifTeamRepository.findByYearAndSemester(
                recruitment.getActYear(), recruitment.getActSemester());
        List<Long> userIds = team.stream().map(member -> member.getUser().getId()).toList();
        Map<Long, StaffRole> roles = staffAccountRepository.findStaffRolesByUserIds(userIds);
        Map<Long, StaffReviewerStatus> grantedStatuses = staffRecruitmentReviewerRepository
                .findAllByRecruitmentId(recruitmentId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        reviewer -> reviewer.getUser().getId(),
                        reviewer -> reviewer.status(now)));

        return team.stream()
                .map(member -> {
                    User user = member.getUser();
                    return new StaffReviewerCandidateInfo(
                            user.getId(),
                            user.getUserName(),
                            departmentNameOf(user),
                            member.getClubDepartment(),
                            roles.get(user.getId()) == StaffRole.ADMIN,
                            grantedStatuses.get(user.getId()));
                })
                .toList();
    }

    /** 회차의 열람 권한 이력 — 회수·만료된 권한도 함께 보여 준다. */
    @Transactional(readOnly = true)
    public List<StaffReviewerInfo> getReviewers(Integer recruitmentId) {
        requireRecruitment(recruitmentId);
        LocalDateTime now = LocalDateTime.now();

        return staffRecruitmentReviewerRepository.findAllByRecruitmentId(recruitmentId).stream()
                .map(reviewer -> StaffReviewerInfo.from(reviewer, affiliationOf(reviewer.getUser().getId()), now))
                .toList();
    }

    @Transactional
    public StaffReviewerInfo grant(Integer recruitmentId, Long granterUserId, Long targetUserId,
                                   LocalDateTime expiresAt) {
        StaffRecruitment recruitment = requireRecruitment(recruitmentId);
        LocalDateTime now = LocalDateTime.now();

        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
        requireGrantable(recruitment, recruitmentId, targetUserId);

        StaffRecruitmentReviewer reviewer = staffRecruitmentReviewerRepository
                .findByRecruitmentIdAndUserIdForUpdate(recruitmentId, targetUserId)
                .orElse(null);

        if (reviewer == null) {
            User target = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));
            reviewer = staffRecruitmentReviewerRepository.save(
                    StaffRecruitmentReviewer.grant(recruitment, target, granterUserId, now, expiresAt));
        } else if (reviewer.isActive(now)) {
            throw new ForifException(ErrorCode.STAFF_REVIEWER_ALREADY_EXISTS);
        } else {
            // 회수·만료된 권한은 같은 행에서 다시 부여한다.
            reviewer.regrant(granterUserId, now, expiresAt);
        }

        try {
            staffRecruitmentReviewerRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ForifException(ErrorCode.STAFF_REVIEWER_ALREADY_EXISTS);
        }
        return StaffReviewerInfo.from(reviewer, affiliationOf(targetUserId), now);
    }

    /** 회수는 멱등이다. 권한이 없거나 이미 회수됐어도 같은 결과로 끝난다. */
    @Transactional
    public void revoke(Integer recruitmentId, Long revokerUserId, Long targetUserId) {
        requireRecruitment(recruitmentId);
        staffRecruitmentReviewerRepository.findByRecruitmentIdAndUserIdForUpdate(recruitmentId, targetUserId)
                .ifPresent(reviewer -> reviewer.revoke(revokerUserId, LocalDateTime.now()));
    }

    // ==================== 열람 권한 검사 ====================

    /**
     * 지원서 목록·상세 접근 검사. 회장단이거나 해당 회차의 유효한 열람 권한이 있어야 한다.
     * 같은 회차에 지원한 사람은 권한이 있어도 열람할 수 없다.
     */
    @Transactional(readOnly = true)
    public void requireApplicationAccess(Long userId, Integer recruitmentId) {
        if (staffAccountService.isPresidentTeam(userId)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean active = staffRecruitmentReviewerRepository
                .findByRecruitmentIdAndUserId(recruitmentId, userId)
                .map(reviewer -> reviewer.isActive(now))
                .orElse(false);
        if (!active) {
            throw new ForifException(ErrorCode.STAFF_REVIEWER_NOT_ALLOWED);
        }
        if (staffApplicationRepository.existsByRecruitmentIdAndApplicantId(recruitmentId, userId)) {
            throw new ForifException(ErrorCode.STAFF_REVIEWER_NOT_ALLOWED);
        }
    }

    /** 열람 권한이 있는 회차 ID. 회장단은 전체 회차를 보므로 빈 값을 돌려주고 호출부에서 구분한다. */
    @Transactional(readOnly = true)
    public List<Integer> getReviewableRecruitmentIds(Long userId) {
        return staffRecruitmentReviewerRepository.findActiveRecruitmentIdsByUserId(userId);
    }

    // ==================== 내부 ====================

    private StaffRecruitment requireRecruitment(Integer recruitmentId) {
        return staffRecruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND));
    }

    /**
     * 부여 가능 대상인지 확인한다.
     * 회차 학기의 운영진이면서 ADMIN 계정이 있어야 하고, 같은 회차 지원자는 제외한다.
     */
    private void requireGrantable(StaffRecruitment recruitment, Integer recruitmentId, Long targetUserId) {
        boolean teamMember = forifTeamRepository.existsByActYearAndActSemesterAndUserId(
                recruitment.getActYear(), recruitment.getActSemester(), targetUserId);
        if (!teamMember) {
            throw new ForifException(ErrorCode.STAFF_REVIEWER_CONFLICT);
        }
        if (!staffAccountRepository.existsByUserIdAndRole(targetUserId, StaffRole.ADMIN)) {
            throw new ForifException(ErrorCode.STAFF_REVIEWER_CONFLICT);
        }
        if (staffApplicationRepository.existsByRecruitmentIdAndApplicantId(recruitmentId, targetUserId)) {
            throw new ForifException(ErrorCode.STAFF_REVIEWER_CONFLICT);
        }
    }

    private String affiliationOf(Long userId) {
        return staffAccountRepository.findByUserId(userId)
                .map(account -> account.getAffiliation())
                .orElse(null);
    }

    private String departmentNameOf(User user) {
        return Optional.ofNullable(user.getDepartmentEntity())
                .map(department -> department.getDepartmentName())
                .orElseGet(user::getDepartment);
    }
}
