package org.forif_backend.infrastructure.persistence.halloffame;

import org.forif_backend.domain.halloffame.HallOfFameEntry;
import org.forif_backend.domain.halloffame.HallOfFameStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HallOfFameJpaRepository extends JpaRepository<HallOfFameEntry, Integer> {

    // 성취는 상세 응답에서 항상 쓰므로 함께 읽는다. badges는 @OrderColumn 컬렉션이라 지연 로딩으로 둔다.
    @Query("""
            SELECT DISTINCT e FROM HallOfFameEntry e
            LEFT JOIN FETCH e.achievements
            WHERE e.id = :entryId
            """)
    Optional<HallOfFameEntry> findDetailById(@Param("entryId") Integer entryId);

    @Query("""
            SELECT DISTINCT e FROM HallOfFameEntry e
            LEFT JOIN FETCH e.achievements
            WHERE e.slug = :slug AND e.status = org.forif_backend.domain.halloffame.HallOfFameStatus.PUBLISHED
            """)
    Optional<HallOfFameEntry> findPublishedBySlug(@Param("slug") String slug);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT e FROM HallOfFameEntry e
            WHERE (:status IS NULL OR e.status = :status)
            ORDER BY e.displayOrder ASC, e.id DESC
            """)
    List<HallOfFameEntry> findAllForAdmin(@Param("status") HallOfFameStatus status, Pageable pageable);

    @Query("""
            SELECT COUNT(e) FROM HallOfFameEntry e
            WHERE (:status IS NULL OR e.status = :status)
            """)
    long countForAdmin(@Param("status") HallOfFameStatus status);

    @Query("""
            SELECT e FROM HallOfFameEntry e
            WHERE e.status = org.forif_backend.domain.halloffame.HallOfFameStatus.PUBLISHED
              AND (:actYear IS NULL OR e.actYear = :actYear)
              AND (:actSemester IS NULL OR e.actSemester = :actSemester)
            ORDER BY e.displayOrder ASC, e.id DESC
            """)
    List<HallOfFameEntry> findPublished(@Param("actYear") Integer actYear,
                                        @Param("actSemester") Integer actSemester,
                                        Pageable pageable);

    @Query("""
            SELECT COUNT(e) FROM HallOfFameEntry e
            WHERE e.status = org.forif_backend.domain.halloffame.HallOfFameStatus.PUBLISHED
              AND (:actYear IS NULL OR e.actYear = :actYear)
              AND (:actSemester IS NULL OR e.actSemester = :actSemester)
            """)
    long countPublished(@Param("actYear") Integer actYear, @Param("actSemester") Integer actSemester);
}
