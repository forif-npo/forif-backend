package org.forif_backend.domain.staffrecruitment;

import java.util.List;
import java.util.Optional;

public interface StaffRecruitmentRepository {

    StaffRecruitment save(StaffRecruitment recruitment);

    /** 유니크 제약 위반을 서비스 계층에서 바로 처리할 수 있도록 변경을 DB에 반영한다. */
    void flush();

    Optional<StaffRecruitment> findById(Integer recruitmentId);

    /** 부서·문항까지 함께 읽는 상세 조회 (N+1 방지) */
    Optional<StaffRecruitment> findDetailById(Integer recruitmentId);

    /** 게시·마감처럼 상태를 바꾸는 작업에서 행을 잠근 채 조회한다. */
    Optional<StaffRecruitment> findByIdForUpdate(Integer recruitmentId);

    /** 현재 게시 중인 회차. 계약상 최대 한 건이다. */
    Optional<StaffRecruitment> findPublished();

    /** 어드민 목록 — 상태 필터는 null이면 전체, 정렬은 recruitment_id DESC 고정 */
    List<StaffRecruitment> findAllForAdmin(StaffRecruitmentStatus status, int offset, int limit);

    long countForAdmin(StaffRecruitmentStatus status);
}
