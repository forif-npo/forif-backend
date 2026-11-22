package org.forif_backend.infrastructure.persistence.staffrecruitment;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.domain.staffrecruitment.StaffInterviewPlan;
import org.forif_backend.domain.staffrecruitment.StaffInterviewPlanRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, StaffInterviewPlanRepositoryImpl.class})
class StaffInterviewPlanVersionTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private StaffInterviewPlanRepository repository;

    @Test
    void 날짜_순서만_바꿔도_버전이_증가한다() {
        LocalDate first = LocalDate.of(2026, 12, 1);
        LocalDate second = first.plusDays(1);
        StaffRecruitment recruitment = StaffRecruitment.create(2026, 2, "운영진 모집", null, null,
                LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 12, 1, 0, 0));
        entityManager.persist(recruitment);
        StaffInterviewPlan plan = StaffInterviewPlan.create(recruitment, 9, 22, 30, true, null,
                List.of(first, second));
        entityManager.persist(plan);
        entityManager.flush();
        entityManager.clear();

        StaffInterviewPlan loaded = entityManager.find(StaffInterviewPlan.class, plan.getId());
        Long originalVersion = loaded.getVersion();
        repository.incrementVersion(loaded);
        loaded.update(9, 22, 30, true, null, List.of(second, first));
        entityManager.flush();

        assertThat(loaded.getVersion()).isGreaterThan(originalVersion);
        assertThat(loaded.candidateDates()).containsExactly(second, first);
    }
}
