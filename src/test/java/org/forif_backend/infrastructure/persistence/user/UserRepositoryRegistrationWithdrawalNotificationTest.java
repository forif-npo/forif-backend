package org.forif_backend.infrastructure.persistence.user;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.domain.dues.MemberSemesterCheck;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.infrastructure.persistence.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class, UserRepositoryImpl.class})
class UserRepositoryRegistrationWithdrawalNotificationTest {

    private static final int YEAR = 2026;
    private static final int SEMESTER = 2;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepositoryImpl userRepository;

    @Test
    void excludesRegistrationWithdrawnUsersFromDuesAndGoogleFormReminderRecipients() {
        User eligible = persistAcceptedUser(20260001L, "대상자", 1);
        User withdrawn = persistAcceptedUser(20260002L, "철회자", 2);
        MemberSemesterCheck withdrawnCheck = MemberSemesterCheck.create(withdrawn, YEAR, SEMESTER);
        withdrawnCheck.withdrawRegistration();
        entityManager.persist(withdrawnCheck);
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.searchAcceptedUsersMissingDuesByYearSemester(
                YEAR, SEMESTER, null, 20, null))
                .extracting(User::getId)
                .containsExactly(eligible.getId());
        assertThat(userRepository.searchAcceptedUsersMissingGoogleFormByYearSemester(
                YEAR, SEMESTER, null, 20, null))
                .extracting(User::getId)
                .containsExactly(eligible.getId());
        assertThat(userRepository.countAcceptedUsersMissingDuesByYearSemester(YEAR, SEMESTER, null))
                .isEqualTo(1);
        assertThat(userRepository.countAcceptedUsersMissingGoogleFormByYearSemester(YEAR, SEMESTER, null))
                .isEqualTo(1);
    }

    private User persistAcceptedUser(Long id, String name, int studyId) {
        User user = User.createUser(id, name, id + "@forif.org", "010-0000-0000", "컴퓨터공학과");
        entityManager.persist(user);
        UserApply application = UserApply.applyStudy(user, study(studyId, "스터디" + studyId), "지원 사유", YEAR, SEMESTER);
        application.updateStatus(studyId, UserApplyStatus.ACCEPT);
        entityManager.persist(application);
        return user;
    }

    private Study study(int id, String name) {
        Study study = mock(Study.class);
        when(study.getId()).thenReturn(id);
        when(study.getStudyName()).thenReturn(name);
        return study;
    }
}
