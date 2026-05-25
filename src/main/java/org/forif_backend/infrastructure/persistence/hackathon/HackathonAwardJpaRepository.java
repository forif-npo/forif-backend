package org.forif_backend.infrastructure.persistence.hackathon;

import org.forif_backend.domain.hackathon.HackathonAward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HackathonAwardJpaRepository extends JpaRepository<HackathonAward, Long> {

    Optional<HackathonAward> findByHackathonIdAndId(Long hackathonId, Long id);

    List<HackathonAward> findByHackathonIdOrderByAwardRankAscIdAsc(Long hackathonId);
}
