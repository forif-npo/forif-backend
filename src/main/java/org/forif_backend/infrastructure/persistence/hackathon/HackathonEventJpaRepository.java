package org.forif_backend.infrastructure.persistence.hackathon;

import org.forif_backend.domain.hackathon.HackathonEvent;
import org.forif_backend.domain.hackathon.HackathonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HackathonEventJpaRepository extends JpaRepository<HackathonEvent, Long> {

    boolean existsByHeldYearAndHeldSemesterAndEventRound(int heldYear, int heldSemester, int eventRound);

    boolean existsByDeletedAtIsNullAndStatusNot(HackathonStatus status);

    List<HackathonEvent> findByDeletedAtIsNullAndStatusNot(HackathonStatus status);

    @Query("""
            SELECT h FROM HackathonEvent h
            WHERE h.deletedAt IS NULL
              AND (:year IS NULL OR h.heldYear = :year)
              AND (:semester IS NULL OR h.heldSemester = :semester)
              AND (:status IS NULL OR h.status = :status)
            ORDER BY h.startsAt DESC, h.id DESC
            """)
    List<HackathonEvent> search(@Param("year") Integer year,
                                @Param("semester") Integer semester,
                                @Param("status") HackathonStatus status);
}
