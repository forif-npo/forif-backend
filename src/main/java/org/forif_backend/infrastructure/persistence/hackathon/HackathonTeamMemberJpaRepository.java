package org.forif_backend.infrastructure.persistence.hackathon;

import org.forif_backend.domain.hackathon.HackathonTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HackathonTeamMemberJpaRepository extends JpaRepository<HackathonTeamMember, Long> {

    Optional<HackathonTeamMember> findByHackathonIdAndUserId(Long hackathonId, Long userId);

    Optional<HackathonTeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    List<HackathonTeamMember> findByTeamIdOrderByJoinedAtAsc(Long teamId);

    long countByTeamId(Long teamId);

    void deleteByTeamId(Long teamId);
}
