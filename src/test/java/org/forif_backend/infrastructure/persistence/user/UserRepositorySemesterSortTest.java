package org.forif_backend.infrastructure.persistence.user;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.dues.MemberSemesterCheck;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.infrastructure.persistence.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class, UserRepositoryImpl.class, UserApplyRepositoryImpl.class})
class UserRepositorySemesterSortTest {

    private static final int YEAR = 2026;
    private static final int SEMESTER = 1;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepositoryImpl userRepository;

    @Autowired
    private UserApplyRepositoryImpl userApplyRepository;

    @Test
    void ordersOnlySelectedSemesterMembersBeforeApplyingTheOffset() {
        User 다람 = persistUser(910001L, "다람");
        User 가람 = persistUser(910002L, "가람");
        User 나람 = persistUser(910003L, "나람");
        User 이전학기부원 = persistUser(910004L, "가장먼저");
        다람.updateProfile("A학과", null);
        가람.updateProfile("B학과", null);
        나람.updateProfile("A학과", null);
        addMember(다람, YEAR, SEMESTER, "다람 스터디");
        addMember(가람, YEAR, SEMESTER, "가람 스터디");
        addMember(나람, YEAR, SEMESTER, "나람 스터디");
        addMember(이전학기부원, 2025, 2, "이전 학기 스터디");
        entityManager.flush();
        entityManager.clear();

        List<SortCriteria> sorting = List.of(new SortCriteria("userName", SortDirection.ASC));
        List<User> firstPage = userRepository.searchUsersByYearSemesterWithOffset(
                YEAR, SEMESTER, 0, 2, null, sorting);
        List<User> secondPage = userRepository.searchUsersByYearSemesterWithOffset(
                YEAR, SEMESTER, 1, 2, null, sorting);

        assertThat(firstPage).extracting(User::getUserName).containsExactly("가람", "나람");
        assertThat(secondPage).extracting(User::getUserName).containsExactly("다람");

        assertThat(userRepository.searchUsersByYearSemesterWithOffset(
                YEAR, SEMESTER, 0, 10, null,
                List.of(new SortCriteria("department", SortDirection.ASC))))
                .extracting(User::getUserName)
                .containsExactly("나람", "다람", "가람");
        assertThat(userRepository.searchUsersByYearSemesterWithOffset(
                YEAR, SEMESTER, 0, 10, null,
                List.of(new SortCriteria("userId", SortDirection.ASC))))
                .extracting(User::getUserName)
                .containsExactly("다람", "가람", "나람");

        assertThat(userRepository.searchUsersByYearSemesterWithOffset(
                YEAR, SEMESTER, 0, 10, null,
                List.of(
                        new SortCriteria("department", SortDirection.ASC),
                        new SortCriteria("userName", SortDirection.DESC)
                )))
                .extracting(User::getUserName)
                .containsExactly("다람", "나람", "가람");
    }

    @Test
    void separatesAcceptedAndFullyRejectedApplicantsAndExcludesPendingApplicants() {
        User primaryAccepted = persistUser(920001L, "1순위 합격");
        User secondaryAccepted = persistUser(920002L, "2순위 합격");
        User primaryRejected = persistUser(920003L, "1순위 불합격");
        User fullyRejected = persistUser(920004L, "모두 불합격");
        User pending = persistUser(920005L, "대기중");
        User secondaryPending = persistUser(920006L, "2순위 대기");
        User previousSemesterRejected = persistUser(920007L, "이전 학기 불합격");
        User acceptedThenRemoved = persistUser(920008L, "합격 후 명단 제외");
        User autonomousAccepted = persistUser(920009L, "자율부원 합격");
        User primaryAcceptedSecondaryPending = persistUser(920010L, "1순위 합격 2순위 대기");

        persistApplication(primaryAccepted, 1, UserApplyStatus.ACCEPT, null);
        persistApplication(secondaryAccepted, 2, UserApplyStatus.REJECT, UserApplyStatus.ACCEPT);
        persistApplication(primaryRejected, 3, UserApplyStatus.REJECT, null);
        persistApplication(fullyRejected, 4, UserApplyStatus.REJECT, UserApplyStatus.REJECT);
        persistApplication(pending, 5, UserApplyStatus.PENDING, null);
        persistApplication(secondaryPending, 6, UserApplyStatus.REJECT, UserApplyStatus.PENDING);
        persistApplication(previousSemesterRejected, 7, 2025, 2, UserApplyStatus.REJECT, null);
        persistApplication(acceptedThenRemoved, 8, UserApplyStatus.REJECT, null);
        persistAutonomousAcceptedApplication(autonomousAccepted);
        persistApplication(primaryAcceptedSecondaryPending, 10, UserApplyStatus.ACCEPT, UserApplyStatus.PENDING);
        // 합격 확인 이력은 부원 명단 삭제 후에도 남아 심사 불합격과 구분한다.
        entityManager.persist(MemberSemesterCheck.create(acceptedThenRemoved, YEAR, SEMESTER));

        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.searchRegularStudyAcceptedApplicantsByYearSemester(
                YEAR, SEMESTER, null, 10, null))
                .extracting(User::getId)
                .containsExactlyInAnyOrder(920001L, 920002L, 920010L);
        assertThat(userRepository.countRegularStudyAcceptedApplicantsByYearSemester(YEAR, SEMESTER, null))
                .isEqualTo(3);

        assertThat(userRepository.searchAutonomousStudyAcceptedApplicantsByYearSemester(
                YEAR, SEMESTER, null, 10, null))
                .extracting(User::getId)
                .containsExactly(920009L);
        assertThat(userRepository.countAutonomousStudyAcceptedApplicantsByYearSemester(YEAR, SEMESTER, null))
                .isEqualTo(1);

        assertThat(userRepository.searchApplicantsByYearSemester(YEAR, SEMESTER, null, 10, null))
                .extracting(User::getId)
                .containsExactlyInAnyOrder(920001L, 920002L, 920003L, 920004L, 920009L, 920010L);
        assertThat(userRepository.countApplicantsByYearSemester(YEAR, SEMESTER, null))
                .isEqualTo(6);

        Map<Long, String> acceptedStudyNames = userApplyRepository
                .findAcceptedStudyNamesByUserIdsAndYearSemester(
                        List.of(920001L, 920002L, 920003L, 920004L, 920005L, 920006L, 920010L), YEAR, SEMESTER);
        assertThat(acceptedStudyNames)
                .containsEntry(920001L, "스터디 1")
                .containsEntry(920002L, "스터디 102")
                .containsEntry(920010L, "스터디 10")
                .doesNotContainKeys(920003L, 920004L, 920005L, 920006L);

        assertThat(userRepository.searchRejectedApplicantsByYearSemester(
                YEAR, SEMESTER, null, 10, null))
                .extracting(User::getId)
                .containsExactlyInAnyOrder(920003L, 920004L);
        assertThat(userRepository.countRejectedApplicantsByYearSemester(YEAR, SEMESTER, null))
                .isEqualTo(2);
    }

    private User persistUser(Long id, String name) {
        User user = User.createUser(id, name, id + "@forif.org", "010-0000-0000", "컴퓨터공학과");
        entityManager.persist(user);
        return user;
    }

    private void addMember(User member, int year, int semester, String studyName) {
        User mentor = persistUser(member.getId() + 100_000L, "멘토" + member.getId());
        Study study = Study.createPendingStudy(mentor, year, semester);
        study.setStudyName(studyName);
        study.approve();
        entityManager.persist(study);
        entityManager.persist(StudyUser.create(study, member));
    }

    private void persistApplication(
            User user,
            int studyId,
            UserApplyStatus primaryStatus,
            UserApplyStatus secondaryStatus
    ) {
        persistApplication(user, studyId, YEAR, SEMESTER, primaryStatus, secondaryStatus);
    }

    private void persistApplication(
            User user,
            int studyId,
            int year,
            int semester,
            UserApplyStatus primaryStatus,
            UserApplyStatus secondaryStatus
    ) {
        Study primaryStudy = mock(Study.class);
        when(primaryStudy.getId()).thenReturn(studyId);
        when(primaryStudy.getStudyName()).thenReturn("스터디 " + studyId);

        UserApply application = UserApply.applyStudy(user, primaryStudy, "지원 사유", year, semester);
        application.updateStatus(studyId, primaryStatus);
        if (secondaryStatus != null) {
            int secondaryStudyId = studyId + 100;
            application.addSecondaryStudy(secondaryStudyId, "스터디 " + secondaryStudyId, "지원 사유");
            application.updateStatus(secondaryStudyId, secondaryStatus);
        }
        entityManager.persist(application);
    }

    private void persistAutonomousAcceptedApplication(User user) {
        User mentor = persistUser(user.getId() + 100_000L, "자율 멘토" + user.getId());
        Study autonomousStudy = Study.createAutonomousStudy(mentor, YEAR, SEMESTER);
        entityManager.persist(autonomousStudy);
        entityManager.flush();

        UserApply application = UserApply.applyStudy(user, autonomousStudy, "지원 사유", YEAR, SEMESTER);
        application.updateStatus(autonomousStudy.getId(), UserApplyStatus.ACCEPT);
        entityManager.persist(application);
    }
}
