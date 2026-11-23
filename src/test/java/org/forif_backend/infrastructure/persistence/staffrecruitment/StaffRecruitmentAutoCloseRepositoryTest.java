package org.forif_backend.infrastructure.persistence.staffrecruitment;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class StaffRecruitmentAutoCloseRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private StaffRecruitmentJpaRepository repository;

    @Test
    void 게시_회차는_종료_시각부터_자동_마감_대상이_된다() {
        LocalDateTime endsAt = LocalDateTime.of(2026, 11, 24, 0, 0);
        StaffRecruitment recruitment = StaffRecruitment.create(2027, 1, "운영진 모집", null, null,
                endsAt.minusDays(1), endsAt);
        recruitment.publish();
        entityManager.persist(recruitment);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findExpiredPublishedForUpdate(endsAt.minusNanos(1))).isEmpty();
        StaffRecruitment expired = repository.findExpiredPublishedForUpdate(endsAt).orElseThrow();
        expired.close("모집 기간 종료", endsAt);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findExpiredPublishedForUpdate(endsAt.plusSeconds(1))).isEmpty();
    }
}
