package org.forif_backend.infrastructure.persistence.team;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, ForifTeamRepositoryImpl.class})
class ForifTeamRepositorySemesterTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ForifTeamRepositoryImpl forifTeamRepository;

    @Test
    void returnsOnlyOperatorsFromTheRequestedSemester() {
        User currentSemesterOperator = persistUser(920001L, "현재 운영진");
        User pastSemesterOperator = persistUser(920002L, "이전 운영진");
        entityManager.persist(ForifTeam.create(currentSemesterOperator, 2026, 1, "개발팀"));
        entityManager.persist(ForifTeam.create(pastSemesterOperator, 2025, 2, "디자인팀"));
        entityManager.flush();
        entityManager.clear();

        List<ForifTeam> operators = forifTeamRepository.findByYearAndSemester(2026, 1);

        assertThat(operators).extracting(team -> team.getUser().getUserName())
                .containsExactly("현재 운영진");
    }

    private User persistUser(Long id, String name) {
        User user = User.createUser(id, name, id + "@forif.org", "010-0000-0000", "컴퓨터공학과");
        entityManager.persist(user);
        return user;
    }
}
