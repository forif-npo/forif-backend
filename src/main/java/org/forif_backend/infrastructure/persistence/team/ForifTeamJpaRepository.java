package org.forif_backend.infrastructure.persistence.team;

import org.forif_backend.domain.team.ForifTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForifTeamJpaRepository extends JpaRepository<ForifTeam, Long> {

    List<ForifTeam> findByActYearAndActSemester(int actYear, int actSemester);

    boolean existsByActYearAndActSemesterAndUser_Id(int actYear, int actSemester, Long userId);
}
