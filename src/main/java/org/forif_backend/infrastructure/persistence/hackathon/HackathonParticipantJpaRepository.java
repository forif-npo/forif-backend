package org.forif_backend.infrastructure.persistence.hackathon;

import org.forif_backend.domain.hackathon.HackathonParticipant;
import org.forif_backend.domain.hackathon.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HackathonParticipantJpaRepository extends JpaRepository<HackathonParticipant, Long> {

    Optional<HackathonParticipant> findByHackathonIdAndUserId(Long hackathonId, Long userId);

    List<HackathonParticipant> findByHackathonIdAndStatus(Long hackathonId, ParticipantStatus status);

    List<HackathonParticipant> findByHackathonId(Long hackathonId);

    @Query("""
            SELECT p FROM HackathonParticipant p
            WHERE p.hackathon.id = :hackathonId
              AND (:status IS NULL OR p.status = :status)
              AND NOT EXISTS (
                  SELECT 1 FROM HackathonTeamMember tm
                  WHERE tm.hackathon.id = :hackathonId
                    AND tm.user.id = p.user.id
              )
            ORDER BY p.registeredAt ASC
            """)
    List<HackathonParticipant> findParticipantsWithoutTeam(@Param("hackathonId") Long hackathonId,
                                                           @Param("status") ParticipantStatus status);
}
