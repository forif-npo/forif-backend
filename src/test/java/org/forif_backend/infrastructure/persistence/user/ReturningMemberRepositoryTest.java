package org.forif_backend.infrastructure.persistence.user;

import jakarta.persistence.EntityManager;
import org.forif_backend.application.user.dto.ReturningMemberRoster;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.domain.department.College;
import org.forif_backend.domain.department.Department;
import org.forif_backend.domain.dues.MemberSemesterCheck;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.infrastructure.persistence.config.QueryDslConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class, UserRepositoryImpl.class})
class ReturningMemberRepositoryTest {
    @Autowired EntityManager em;
    @Autowired UserRepositoryImpl repository;

    @ParameterizedTest
    @CsvSource({"2026,1,2025,2", "2026,2,2026,1"})
    void exportsOnlyPreviousMembersAcceptedThisSemesterWithoutDuplicates(
            int year, int semester, int previousYear, int previousSemester) {
        User primary = user(910001L, "가상가");
        User secondary = user(910002L, "가상가");
        User autonomous = user(910003L, "가상나");
        User pending = user(910004L, "대기");
        User rejected = user(910005L, "불합격");
        User withdrawn = user(910006L, "철회");
        User newcomer = user(910007L, "신규");
        User olderMember = user(910008L, "이전이전학기");
        User oldApplicant = user(910009L, "과거신청");
        User noApplication = user(910010L, "미신청");
        Study previous = study(primary, previousYear, previousSemester, false);
        for (User user : List.of(primary, secondary, autonomous, pending, rejected, withdrawn, oldApplicant, noApplication)) {
            em.persist(StudyUser.create(previous, user));
        }
        // 여러 스터디 활동 이력은 한 명으로 집계한다.
        em.persist(StudyUser.create(study(primary, previousYear, previousSemester, false), primary));
        em.persist(StudyUser.create(study(olderMember, previousYear - 1, previousSemester, false), olderMember));
        Study current = study(primary, year, semester, false);
        UserApply primaryApplication = apply(primary, current, year, semester, UserApplyStatus.ACCEPT);
        UserApply secondaryApplication = apply(secondary, current, year, semester, UserApplyStatus.REJECT);
        Study secondChoice = study(primary, year, semester, false);
        // 다른 순위가 대기 중이어도 이미 합격한 사람은 포함한다.
        primaryApplication.addSecondaryStudy(secondChoice.getId(), "가상 2순위", "가상 사유");
        secondaryApplication.addSecondaryStudy(secondChoice.getId(), "가상 2순위", "가상 사유");
        secondaryApplication.updateStatus(secondChoice.getId(), UserApplyStatus.ACCEPT);
        apply(autonomous, study(autonomous, year, semester, true), year, semester, UserApplyStatus.ACCEPT);
        apply(pending, current, year, semester, UserApplyStatus.PENDING);
        apply(rejected, current, year, semester, UserApplyStatus.REJECT);
        apply(withdrawn, current, year, semester, UserApplyStatus.ACCEPT);
        apply(newcomer, current, year, semester, UserApplyStatus.ACCEPT);
        apply(olderMember, current, year, semester, UserApplyStatus.ACCEPT);
        apply(oldApplicant, previous, previousYear, previousSemester, UserApplyStatus.ACCEPT);
        MemberSemesterCheck withdrawal = MemberSemesterCheck.create(withdrawn, year, semester);
        withdrawal.withdrawRegistration();
        em.persist(withdrawal);
        // 회비 미납·폼 미제출, 연락처 누락도 명부에서 제외할 사유가 아니다.
        em.persist(MemberSemesterCheck.create(primary, year, semester));
        secondary.updatePhoneNum(null);
        // 과거 학기의 철회 여부는 현재 학기 합격 판정에 영향을 주지 않는다.
        MemberSemesterCheck oldWithdrawal = MemberSemesterCheck.create(autonomous, previousYear - 1, previousSemester);
        oldWithdrawal.withdrawRegistration();
        em.persist(oldWithdrawal);
        em.flush();
        em.clear();

        assertThat(repository.findReturningMembers(year, semester, previousYear, previousSemester))
                .extracting(User::getId).containsExactly(primary.getId(), secondary.getId(), autonomous.getId());
        assertThat(repository.findReturningMembers(year + 3, semester, year + 2, previousSemester)).isEmpty();
    }

    @Test
    void fetchesLinkedCollegeAndDepartmentAlongsideLegacyMembers() {
        College college = BeanUtils.instantiateClass(College.class);
        ReflectionTestUtils.setField(college, "collegeName", "가상대학");
        ReflectionTestUtils.setField(college, "sourceCode", "TEST-C");
        em.persist(college);
        Department department = BeanUtils.instantiateClass(Department.class);
        ReflectionTestUtils.setField(department, "college", college);
        ReflectionTestUtils.setField(department, "departmentName", "연결학과");
        em.persist(department);
        User linked = user(920001L, "가상가");
        linked.updateDepartment(department);
        User legacy = user(920002L, "가상나");
        Study previous = study(linked, 2025, 2, false);
        Study current = study(linked, 2026, 1, false);
        for (User member : List.of(linked, legacy)) {
            em.persist(StudyUser.create(previous, member));
            apply(member, current, 2026, 1, UserApplyStatus.ACCEPT);
        }
        em.flush();
        em.clear();

        List<User> members = repository.findReturningMembers(2026, 1, 2025, 2);
        em.clear();

        assertThat(members.stream().map(ReturningMemberRoster.Member::from).toList())
                .containsExactly(
                        new ReturningMemberRoster.Member("920001", "가상가", "가상대학", "연결학과", "01000000000"),
                        new ReturningMemberRoster.Member("920002", "가상나", "", "가상학과", "01000000000"));
    }

    private User user(long id, String name) {
        User user = User.createUser(id, name, id + "@example.invalid", "01000000000", "가상학과");
        em.persist(user);
        return user;
    }

    private Study study(User mentor, int year, int semester, boolean autonomous) {
        Study study = autonomous ? Study.createAutonomousStudy(mentor, year, semester)
                : Study.createPendingStudy(mentor, year, semester);
        study.setStudyName("가상 스터디");
        if (!autonomous) study.approve();
        em.persist(study);
        em.flush();
        return study;
    }

    private UserApply apply(User user, Study study, int year, int semester, UserApplyStatus status) {
        UserApply application = UserApply.applyStudy(user, study, "가상 지원 사유", year, semester);
        application.updateStatus(study.getId(), status);
        em.persist(application);
        return application;
    }
}
