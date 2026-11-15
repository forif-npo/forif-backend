package org.forif_backend.domain.staffrecruitment;

import java.util.List;
import java.util.Optional;

public interface StaffRecruitmentReviewerRepository {

    StaffRecruitmentReviewer save(StaffRecruitmentReviewer reviewer);

    void flush();

    /** 회차+대상자 기준 단건. 회수·만료된 행도 포함한다. */
    Optional<StaffRecruitmentReviewer> findByRecruitmentIdAndUserId(Integer recruitmentId, Long userId);

    /** 상태 변경 시 행을 잠근 채 조회한다. */
    Optional<StaffRecruitmentReviewer> findByRecruitmentIdAndUserIdForUpdate(Integer recruitmentId, Long userId);

    /** 회차의 모든 열람 권한 이력 (active·revoked·expired 포함) */
    List<StaffRecruitmentReviewer> findAllByRecruitmentId(Integer recruitmentId);

    /** 특정 사용자가 열람할 수 있는 회차 ID 목록 (회수되지 않고 만료 전) */
    List<Integer> findActiveRecruitmentIdsByUserId(Long userId);
}
