package org.forif_backend.infrastructure.persistence.staffrecruitment;

import jakarta.persistence.LockModeType;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffRecruitmentJpaRepository extends JpaRepository<StaffRecruitment, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM StaffRecruitment r WHERE r.id = :id")
    Optional<StaffRecruitment> findByIdForUpdate(@Param("id") Integer id);

    // 상세 응답은 부서와 문항을 모두 사용하므로 함께 조회한다 (N+1 방지).
    // 컬렉션 두 개를 한 번에 fetch join 하면 MultipleBagFetchException이 나므로 부서만 조인하고
    // 문항은 @OrderBy가 걸린 지연 로딩으로 읽는다.
    @Query("""
            SELECT DISTINCT r FROM StaffRecruitment r
            LEFT JOIN FETCH r.departments
            WHERE r.id = :id
            """)
    Optional<StaffRecruitment> findDetailById(@Param("id") Integer id);

    Optional<StaffRecruitment> findByStatus(StaffRecruitmentStatus status);

    List<StaffRecruitment> findAllByOrderByIdDesc(Pageable pageable);

    List<StaffRecruitment> findAllByStatusOrderByIdDesc(StaffRecruitmentStatus status, Pageable pageable);

    long countByStatus(StaffRecruitmentStatus status);
}
