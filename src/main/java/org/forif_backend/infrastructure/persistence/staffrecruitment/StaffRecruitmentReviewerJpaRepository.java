package org.forif_backend.infrastructure.persistence.staffrecruitment;

import jakarta.persistence.LockModeType;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentReviewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StaffRecruitmentReviewerJpaRepository extends JpaRepository<StaffRecruitmentReviewer, Integer> {

    Optional<StaffRecruitmentReviewer> findByRecruitmentIdAndUserId(Integer recruitmentId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM StaffRecruitmentReviewer r
            WHERE r.recruitment.id = :recruitmentId AND r.user.id = :userId
            """)
    Optional<StaffRecruitmentReviewer> findByRecruitmentIdAndUserIdForUpdate(
            @Param("recruitmentId") Integer recruitmentId,
            @Param("userId") Long userId);

    // 목록 응답에서 이름·학과를 쓰므로 회원을 함께 읽는다 (N+1 방지).
    @Query("""
            SELECT r FROM StaffRecruitmentReviewer r
            JOIN FETCH r.user u
            LEFT JOIN FETCH u.departmentEntity
            WHERE r.recruitment.id = :recruitmentId
            ORDER BY r.id ASC
            """)
    List<StaffRecruitmentReviewer> findAllByRecruitmentId(@Param("recruitmentId") Integer recruitmentId);

    @Query("""
            SELECT r.recruitment.id FROM StaffRecruitmentReviewer r
            WHERE r.user.id = :userId
              AND r.revokedAt IS NULL
              AND r.expiresAt > :now
            ORDER BY r.recruitment.id DESC
            """)
    List<Integer> findActiveRecruitmentIdsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
