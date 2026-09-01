package org.forif_backend.infrastructure.persistence.hackathon;

import jakarta.persistence.LockModeType;
import org.forif_backend.domain.hackathon.HackathonTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface HackathonTeamJpaRepository extends JpaRepository<HackathonTeam, Long> {

    Optional<HackathonTeam> findByHackathonIdAndId(Long hackathonId, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<HackathonTeam> findWithLockByHackathonIdAndId(Long hackathonId, Long id);

    List<HackathonTeam> findByHackathonIdOrderByIdAsc(Long hackathonId);

    boolean existsByHackathonIdAndName(Long hackathonId, String name);
}
