package org.forif_backend.infrastructure.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

@DataJpaTest
@Import({JpaAuditingConfig.class, UserApplyRepositoryImpl.class})
class UserApplyRepositoryImplTest {

    @Autowired private EntityManager entityManager;
    @Autowired private UserApplyRepositoryImpl userApplyRepository;

    @Test
    void rejectsOnlyPendingStatusesInTheSpecifiedSemester() {
        User primaryPending = persistUser(950001L, "1순위 대기");
        User secondaryPending = persistUser(950002L, "2순위 대기");
        User alreadyRejected = persistUser(950003L, "처리 완료");
        User otherSemesterPending = persistUser(950004L, "다른 학기 대기");
        User acceptedPrimaryWithPendingSecondary = persistUser(950005L, "1순위 합격 2순위 대기");

        persistApplication(primaryPending, 1, 2026, 1, UserApplyStatus.PENDING, null);
        persistApplication(secondaryPending, 2, 2026, 1, UserApplyStatus.REJECT, UserApplyStatus.PENDING);
        persistApplication(alreadyRejected, 3, 2026, 1, UserApplyStatus.REJECT, null);
        persistApplication(otherSemesterPending, 4, 2026, 2, UserApplyStatus.PENDING, null);
        persistApplication(acceptedPrimaryWithPendingSecondary, 5, 2026, 1,
                UserApplyStatus.ACCEPT, UserApplyStatus.PENDING);
        entityManager.flush();
        entityManager.clear();

        assertThat(userApplyRepository.rejectPendingApplicationsByYearSemester(2026, 1)).isEqualTo(3);

        assertThat(userApplyRepository.findByApplierIdAndYearSemester(950001L, 2026, 1))
                .get().extracting(UserApply::getPrimaryStatus).isEqualTo(UserApplyStatus.REJECT);
        assertThat(userApplyRepository.findByApplierIdAndYearSemester(950002L, 2026, 1))
                .get().extracting(UserApply::getPrimaryStatus, UserApply::getSecondaryStatus)
                .containsExactly(UserApplyStatus.REJECT, UserApplyStatus.REJECT);
        assertThat(userApplyRepository.findByApplierIdAndYearSemester(950003L, 2026, 1))
                .get().extracting(UserApply::getPrimaryStatus).isEqualTo(UserApplyStatus.REJECT);
        assertThat(userApplyRepository.findByApplierIdAndYearSemester(950004L, 2026, 2))
                .get().extracting(UserApply::getPrimaryStatus).isEqualTo(UserApplyStatus.PENDING);
        assertThat(userApplyRepository.findByApplierIdAndYearSemester(950005L, 2026, 1))
                .get().extracting(UserApply::getPrimaryStatus, UserApply::getSecondaryStatus)
                .containsExactly(UserApplyStatus.ACCEPT, UserApplyStatus.REJECT);
    }

    private User persistUser(Long id, String name) {
        User user = User.createUser(id, name, id + "@forif.org", "010-0000-0000", "컴퓨터공학과");
        entityManager.persist(user);
        return user;
    }

    private void persistApplication(
            User user,
            int primaryStudyId,
            int year,
            int semester,
            UserApplyStatus primaryStatus,
            UserApplyStatus secondaryStatus
    ) {
        Study primaryStudy = mock(Study.class);
        when(primaryStudy.getId()).thenReturn(primaryStudyId);
        when(primaryStudy.getStudyName()).thenReturn("스터디 " + primaryStudyId);

        UserApply application = UserApply.applyStudy(user, primaryStudy, "지원 사유", year, semester);
        application.updateStatus(primaryStudyId, primaryStatus);
        if (secondaryStatus != null) {
            int secondaryStudyId = primaryStudyId + 100;
            application.addSecondaryStudy(secondaryStudyId, "스터디 " + secondaryStudyId, "지원 사유");
            application.updateStatus(secondaryStudyId, secondaryStatus);
        }
        entityManager.persist(application);
    }
}
