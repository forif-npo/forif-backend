package org.forif_backend.application.staffrecruitment;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffReviewerServiceTest {

    private static final Integer RECRUITMENT_ID = 12;
    private static final Long REVIEWER_ID = 2020111111L;
    private static final Long PRESIDENT_ID = 2020999999L;
    private static final Long OUTSIDER_ID = 2020222222L;

    @Mock
    private StaffRecruitmentReviewerRepository staffRecruitmentReviewerRepository;

    @Mock
    private StaffRecruitmentRepository staffRecruitmentRepository;

    @Mock
    private StaffApplicationRepository staffApplicationRepository;

    @Mock
    private StaffAccountRepository staffAccountRepository;

    @Mock
    private ForifTeamRepository forifTeamRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffAccountService staffAccountService;

    @InjectMocks
    private StaffReviewerService staffReviewerService;

    private final User reviewer =
            User.createUser(REVIEWER_ID, "김운영", "staff@hanyang.ac.kr", "01011112222", "컴퓨터소프트웨어학부");

    private StaffRecruitment recruitment() {
        LocalDateTime now = LocalDateTime.now();
        StaffRecruitment recruitment = StaffRecruitment.create(
                2027, 1, "2027-1 운영진 모집", null, null, now.minusDays(1), now.plusDays(1));
        recruitment.publish();
        ReflectionTestUtils.setField(recruitment, "id", RECRUITMENT_ID);
        return recruitment;
    }

    private StaffRecruitmentReviewer grantedReviewer(LocalDateTime expiresAt) {
        return StaffRecruitmentReviewer.grant(
                recruitment(), reviewer, PRESIDENT_ID, LocalDateTime.now().minusHours(1), expiresAt);
    }

    private void givenRecruitment() {
        when(staffRecruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment()));
    }

    private void givenGrantableTarget() {
        when(forifTeamRepository.existsByActYearAndActSemesterAndUserId(2027, 1, REVIEWER_ID)).thenReturn(true);
        when(staffAccountRepository.existsByUserIdAndRole(REVIEWER_ID, StaffRole.ADMIN)).thenReturn(true);
        when(staffApplicationRepository.existsByRecruitmentIdAndApplicantId(RECRUITMENT_ID, REVIEWER_ID))
                .thenReturn(false);
    }

    @Test
    void 후보는_학기_운영진_전원이며_ADMIN_계정_여부를_함께_알려준다() {
        givenRecruitment();
        User withoutAccount = User.createUser(OUTSIDER_ID, "박신입", "new@hanyang.ac.kr", "01033334444", "기계공학부");
        when(forifTeamRepository.findByYearAndSemester(2027, 1)).thenReturn(List.of(
                ForifTeam.create(reviewer, 2027, 1, "개발팀"),
                ForifTeam.create(withoutAccount, 2027, 1, "기획팀")));
        when(staffAccountRepository.findStaffRolesByUserIds(anyList()))
                .thenReturn(Map.of(REVIEWER_ID, StaffRole.ADMIN));
        when(staffRecruitmentReviewerRepository.findAllByRecruitmentId(RECRUITMENT_ID)).thenReturn(List.of());

        List<StaffReviewerCandidateInfo> candidates = staffReviewerService.getCandidates(RECRUITMENT_ID);

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).hasAdminAccount()).isTrue();
        assertThat(candidates.get(0).affiliation()).isEqualTo("개발팀");
        assertThat(candidates.get(0).reviewerStatus()).isNull();
        assertThat(candidates.get(1).hasAdminAccount()).isFalse();
    }

    @Test
    void 회차_학기_운영진이_아니면_부여할_수_없다() {
        givenRecruitment();
        when(forifTeamRepository.existsByActYearAndActSemesterAndUserId(2027, 1, OUTSIDER_ID)).thenReturn(false);

        assertThatThrownBy(() -> staffReviewerService.grant(
                RECRUITMENT_ID, PRESIDENT_ID, OUTSIDER_ID, LocalDateTime.now().plusDays(7)))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_REVIEWER_CONFLICT);
    }

    @Test
    void ADMIN_계정이_없으면_부여할_수_없다() {
        givenRecruitment();
        when(forifTeamRepository.existsByActYearAndActSemesterAndUserId(2027, 1, REVIEWER_ID)).thenReturn(true);
        when(staffAccountRepository.existsByUserIdAndRole(REVIEWER_ID, StaffRole.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> staffReviewerService.grant(
                RECRUITMENT_ID, PRESIDENT_ID, REVIEWER_ID, LocalDateTime.now().plusDays(7)))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_REVIEWER_CONFLICT);
    }

    @Test
    void 같은_회차_지원자에게는_부여할_수_없다() {
        givenRecruitment();
        when(forifTeamRepository.existsByActYearAndActSemesterAndUserId(2027, 1, REVIEWER_ID)).thenReturn(true);
        when(staffAccountRepository.existsByUserIdAndRole(REVIEWER_ID, StaffRole.ADMIN)).thenReturn(true);
        when(staffApplicationRepository.existsByRecruitmentIdAndApplicantId(RECRUITMENT_ID, REVIEWER_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> staffReviewerService.grant(
                RECRUITMENT_ID, PRESIDENT_ID, REVIEWER_ID, LocalDateTime.now().plusDays(7)))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_REVIEWER_CONFLICT);
    }

    @Test
    void 만료_시각이_과거면_부여할_수_없다() {
        givenRecruitment();

        assertThatThrownBy(() -> staffReviewerService.grant(
                RECRUITMENT_ID, PRESIDENT_ID, REVIEWER_ID, LocalDateTime.now().minusMinutes(1)))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 이미_유효한_권한이_있으면_중복_부여를_거절한다() {
        givenRecruitment();
        givenGrantableTarget();
        when(staffRecruitmentReviewerRepository.findByRecruitmentIdAndUserIdForUpdate(RECRUITMENT_ID, REVIEWER_ID))
                .thenReturn(Optional.of(grantedReviewer(LocalDateTime.now().plusDays(3))));

        assertThatThrownBy(() -> staffReviewerService.grant(
                RECRUITMENT_ID, PRESIDENT_ID, REVIEWER_ID, LocalDateTime.now().plusDays(7)))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_REVIEWER_ALREADY_EXISTS);
    }

    @Test
    void 회수된_권한은_같은_행에서_다시_부여한다() {
        givenRecruitment();
        givenGrantableTarget();
        StaffRecruitmentReviewer revoked = grantedReviewer(LocalDateTime.now().plusDays(3));
        revoked.revoke(PRESIDENT_ID, LocalDateTime.now().minusMinutes(10));
        when(staffRecruitmentReviewerRepository.findByRecruitmentIdAndUserIdForUpdate(RECRUITMENT_ID, REVIEWER_ID))
                .thenReturn(Optional.of(revoked));
        when(staffAccountRepository.findByUserId(REVIEWER_ID)).thenReturn(Optional.empty());

        StaffReviewerInfo info = staffReviewerService.grant(
                RECRUITMENT_ID, PRESIDENT_ID, REVIEWER_ID, LocalDateTime.now().plusDays(7));

        assertThat(info.status()).isEqualTo(StaffReviewerStatus.ACTIVE);
        assertThat(info.revokedAt()).isNull();
    }

    @Test
    void 회수는_여러_번_해도_처음_회수_시각을_유지한다() {
        givenRecruitment();
        StaffRecruitmentReviewer granted = grantedReviewer(LocalDateTime.now().plusDays(3));
        when(staffRecruitmentReviewerRepository.findByRecruitmentIdAndUserIdForUpdate(RECRUITMENT_ID, REVIEWER_ID))
                .thenReturn(Optional.of(granted));

        staffReviewerService.revoke(RECRUITMENT_ID, PRESIDENT_ID, REVIEWER_ID);
        LocalDateTime firstRevokedAt = granted.getRevokedAt();
        staffReviewerService.revoke(RECRUITMENT_ID, PRESIDENT_ID, REVIEWER_ID);

        assertThat(granted.getRevokedAt()).isEqualTo(firstRevokedAt);
        assertThat(granted.status(LocalDateTime.now())).isEqualTo(StaffReviewerStatus.REVOKED);
    }

    @Test
    void 권한이_없으면_회수해도_예외가_나지_않는다() {
        givenRecruitment();
        when(staffRecruitmentReviewerRepository.findByRecruitmentIdAndUserIdForUpdate(RECRUITMENT_ID, OUTSIDER_ID))
                .thenReturn(Optional.empty());

        assertThatCode(() -> staffReviewerService.revoke(RECRUITMENT_ID, PRESIDENT_ID, OUTSIDER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void 회장단은_권한_행_없이도_지원서를_열람한다() {
        when(staffAccountService.isPresidentTeam(PRESIDENT_ID)).thenReturn(true);

        assertThatCode(() -> staffReviewerService.requireApplicationAccess(PRESIDENT_ID, RECRUITMENT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void 유효한_권한이_있으면_지원서를_열람한다() {
        when(staffAccountService.isPresidentTeam(REVIEWER_ID)).thenReturn(false);
        when(staffRecruitmentReviewerRepository.findByRecruitmentIdAndUserId(RECRUITMENT_ID, REVIEWER_ID))
                .thenReturn(Optional.of(grantedReviewer(LocalDateTime.now().plusDays(3))));
        when(staffApplicationRepository.existsByRecruitmentIdAndApplicantId(RECRUITMENT_ID, REVIEWER_ID))
                .thenReturn(false);

        assertThatCode(() -> staffReviewerService.requireApplicationAccess(REVIEWER_ID, RECRUITMENT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void 만료된_권한으로는_열람할_수_없다() {
        when(staffAccountService.isPresidentTeam(REVIEWER_ID)).thenReturn(false);
        when(staffRecruitmentReviewerRepository.findByRecruitmentIdAndUserId(RECRUITMENT_ID, REVIEWER_ID))
                .thenReturn(Optional.of(grantedReviewer(LocalDateTime.now().minusMinutes(1))));

        assertThatThrownBy(() -> staffReviewerService.requireApplicationAccess(REVIEWER_ID, RECRUITMENT_ID))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_REVIEWER_NOT_ALLOWED);
    }

    @Test
    void 회수된_권한으로는_열람할_수_없다() {
        when(staffAccountService.isPresidentTeam(REVIEWER_ID)).thenReturn(false);
        StaffRecruitmentReviewer revoked = grantedReviewer(LocalDateTime.now().plusDays(3));
        revoked.revoke(PRESIDENT_ID, LocalDateTime.now().minusMinutes(1));
        when(staffRecruitmentReviewerRepository.findByRecruitmentIdAndUserId(RECRUITMENT_ID, REVIEWER_ID))
                .thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> staffReviewerService.requireApplicationAccess(REVIEWER_ID, RECRUITMENT_ID))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_REVIEWER_NOT_ALLOWED);
    }

    @Test
    void 권한이_아예_없으면_열람할_수_없다() {
        when(staffAccountService.isPresidentTeam(OUTSIDER_ID)).thenReturn(false);
        when(staffRecruitmentReviewerRepository.findByRecruitmentIdAndUserId(RECRUITMENT_ID, OUTSIDER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffReviewerService.requireApplicationAccess(OUTSIDER_ID, RECRUITMENT_ID))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_REVIEWER_NOT_ALLOWED);
    }

    @Test
    void 같은_회차에_지원했다면_권한이_있어도_열람할_수_없다() {
        when(staffAccountService.isPresidentTeam(REVIEWER_ID)).thenReturn(false);
        when(staffRecruitmentReviewerRepository.findByRecruitmentIdAndUserId(RECRUITMENT_ID, REVIEWER_ID))
                .thenReturn(Optional.of(grantedReviewer(LocalDateTime.now().plusDays(3))));
        when(staffApplicationRepository.existsByRecruitmentIdAndApplicantId(RECRUITMENT_ID, REVIEWER_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> staffReviewerService.requireApplicationAccess(REVIEWER_ID, RECRUITMENT_ID))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_REVIEWER_NOT_ALLOWED);
    }

    @Test
    void 새로_부여하면_활성_상태로_저장된다() {
        givenRecruitment();
        givenGrantableTarget();
        when(staffRecruitmentReviewerRepository.findByRecruitmentIdAndUserIdForUpdate(RECRUITMENT_ID, REVIEWER_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(REVIEWER_ID)).thenReturn(Optional.of(reviewer));
        when(staffRecruitmentReviewerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(staffAccountRepository.findByUserId(REVIEWER_ID)).thenReturn(Optional.empty());

        StaffReviewerInfo info = staffReviewerService.grant(
                RECRUITMENT_ID, PRESIDENT_ID, REVIEWER_ID, LocalDateTime.now().plusDays(7));

        assertThat(info.userId()).isEqualTo(REVIEWER_ID);
        assertThat(info.userName()).isEqualTo("김운영");
        assertThat(info.status()).isEqualTo(StaffReviewerStatus.ACTIVE);
        assertThat(info.revokedAt()).isNull();
    }
}
