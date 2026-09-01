package org.forif_backend.infrastructure.persistence.hackathon;

import org.forif_backend.domain.hackathon.HackathonJoinRequest;
import org.forif_backend.domain.hackathon.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HackathonJoinRequestJpaRepository extends JpaRepository<HackathonJoinRequest, Long> {

    Optional<HackathonJoinRequest> findByHackathonIdAndId(Long hackathonId, Long id);

    List<HackathonJoinRequest> findByTeamIdAndStatusOrderByCreatedAtAsc(Long teamId, JoinRequestStatus status);

    List<HackathonJoinRequest> findByTeamIdOrderByCreatedAtAsc(Long teamId);

    boolean existsByHackathonIdAndUserIdAndStatus(Long hackathonId, Long userId, JoinRequestStatus status);
}
