package org.forif_backend.infrastructure.persistence.team;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ForifTeamRepositoryImpl implements ForifTeamRepository {

    private final ForifTeamJpaRepository jpaRepository;

    @Override
    public List<ForifTeam> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<ForifTeam> findByYearAndSemester(int actYear, int actSemester) {
        return jpaRepository.findByActYearAndActSemester(actYear, actSemester);
    }

    @Override
    public Optional<ForifTeam> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public void save(ForifTeam forIfTeam) {
        jpaRepository.save(forIfTeam);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
