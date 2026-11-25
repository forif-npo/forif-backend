package org.forif_backend.infrastructure.persistence.study;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class StudyUserJpaRepositoryHistoryTest {

    @Autowired private EntityManager entityManager;
    @Autowired private StudyUserJpaRepository repository;

    @Test
    void 지난_학기_수강_이력도_지원_자격으로_조회한다() {
        User applicant = User.createUser(2020123456L, "지원자", "applicant@example.com", "01000000000", "예시 학과");
        User other = User.createUser(2020123457L, "다른 회원", "other@example.com", "01000000001", "예시 학과");
        entityManager.persist(applicant);
        entityManager.persist(other);
        Study pastStudy = Study.createPendingStudy(applicant, 2025, 2);
        entityManager.persist(pastStudy);
        entityManager.persist(StudyUser.create(pastStudy, applicant));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.existsByUserId(applicant.getId())).isTrue();
        assertThat(repository.existsByUserId(other.getId())).isFalse();
    }
}
