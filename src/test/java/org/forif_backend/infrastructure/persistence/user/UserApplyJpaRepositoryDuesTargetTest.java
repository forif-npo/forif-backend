package org.forif_backend.infrastructure.persistence.user;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class UserApplyJpaRepositoryDuesTargetTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserApplyJpaRepository userApplyJpaRepository;

    @Test
    void returnsOnlyCurrentSemesterAcceptedApplicants() {
        User primaryAccepted = persistUser(20260001L, "가람");
        User secondaryAccepted = persistUser(20260002L, "나람");
        User additionalPrimaryAccepted = persistUser(20260003L, "다람");
        User pendingApplicant = persistUser(20260004L, "라람");
        User previousSemesterAccepted = persistUser(20250001L, "마람");

        UserApply primaryApplication = UserApply.applyStudy(
                primaryAccepted, study(10, "정규 A"), "지원 사유", 2026, 2);
        primaryApplication.updateStatus(10, UserApplyStatus.ACCEPT);
        entityManager.persist(primaryApplication);

        UserApply secondaryApplication = UserApply.applyStudy(
                secondaryAccepted, study(20, "정규 B"), "지원 사유", 2026, 2);
        secondaryApplication.addSecondaryStudy(21, "정규 C", "지원 사유");
        secondaryApplication.updateStatus(21, UserApplyStatus.ACCEPT);
        entityManager.persist(secondaryApplication);

        UserApply additionalPrimaryApplication = UserApply.applyStudy(
                additionalPrimaryAccepted, study(30, "정규 C"), "지원 사유", 2026, 2);
        additionalPrimaryApplication.updateStatus(30, UserApplyStatus.ACCEPT);
        entityManager.persist(additionalPrimaryApplication);
        entityManager.persist(UserApply.applyStudy(
                pendingApplicant, study(40, "대기 스터디"), "지원 사유", 2026, 2));

        UserApply previousApplication = UserApply.applyStudy(
                previousSemesterAccepted, study(50, "지난 학기"), "지원 사유", 2025, 2);
        previousApplication.updateStatus(50, UserApplyStatus.ACCEPT);
        entityManager.persist(previousApplication);

        entityManager.flush();
        entityManager.clear();

        List<User> result = userApplyJpaRepository.findAcceptedApplicantsByYearSemester(
                2026, 2, null, UserApplyStatus.ACCEPT);

        assertThat(result).extracting(User::getId)
                .containsExactly(20260001L, 20260002L, 20260003L);
    }

    private User persistUser(Long id, String name) {
        User user = User.createUser(id, name, id + "@forif.org", "010-0000-0000", "컴퓨터공학과");
        entityManager.persist(user);
        return user;
    }

    private Study study(int id, String name) {
        Study study = mock(Study.class);
        when(study.getId()).thenReturn(id);
        when(study.getStudyName()).thenReturn(name);
        return study;
    }
}
