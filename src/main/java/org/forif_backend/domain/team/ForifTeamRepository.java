package org.forif_backend.domain.team;

import java.util.List;
import java.util.Optional;

public interface ForifTeamRepository {

    List<ForifTeam> findAll();

    List<ForifTeam> findByYearAndSemester(int actYear, int actSemester);

    Optional<ForifTeam> findById(Long id);

    void save(ForifTeam forIfTeam);

    void deleteById(Long id);
}
