package org.forif_backend.infrastructure.persistence.staffrecruitment;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.domain.staffrecruitment.StaffApplication;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class StaffApplicationJpaRepositoryHistoryTest {

    @Autowired private EntityManager entityManager;
    @Autowired private StaffApplicationJpaRepository repository;

    @Test
    void 본인_지원서만_모든_회차에서_최신순으로_조회한다() {
        User applicant = persistUser(101L);
        User other = persistUser(202L);
        StaffRecruitment past = persistRecruitment(2026, 1, LocalDateTime.of(2026, 5, 1, 12, 0));
        StaffRecruitment recent = persistRecruitment(2026, 2, LocalDateTime.of(2026, 11, 1, 12, 0));
        StaffApplication pastMine = persistApplication(past, applicant, LocalDateTime.of(2026, 5, 1, 12, 0));
        StaffApplication recentMine = persistApplication(recent, applicant, LocalDateTime.of(2026, 11, 1, 12, 0));
        persistApplication(recent, other, LocalDateTime.of(2026, 11, 1, 13, 0));
        entityManager.flush();
        entityManager.clear();

        var history = repository.findByApplicantId(applicant.getId());

        assertThat(history).extracting(StaffApplication::getId)
                .containsExactly(recentMine.getId(), pastMine.getId());
        assertThat(history).allSatisfy(application ->
                assertThat(application.getRecruitment().getActYear()).isEqualTo(2026));
    }

    private User persistUser(Long userId) {
        User user = User.createUser(userId, "테스트 회원", userId + "@example.com", "01000000000", "예시 학과");
        entityManager.persist(user);
        return user;
    }

    private StaffRecruitment persistRecruitment(int year, int semester, LocalDateTime submittedAt) {
        StaffRecruitment recruitment = StaffRecruitment.create(year, semester,
                year + "-" + semester, null, "모집 안내", submittedAt.minusDays(1), submittedAt.plusDays(1));
        recruitment.publish();
        entityManager.persist(recruitment);
        return recruitment;
    }

    private StaffApplication persistApplication(StaffRecruitment recruitment, User applicant, LocalDateTime submittedAt) {
        StaffApplication application = StaffApplication.submit(recruitment, applicant, applicant.getUserName(),
                applicant.getEmail(), applicant.getPhoneNum(), applicant.getDepartment(), submittedAt);
        entityManager.persist(application);
        return application;
    }
}
