package org.forif_backend.domain.staffrecruitment;

import java.util.List;
import java.util.Optional;

public interface StaffApplicationRepository {

    StaffApplication save(StaffApplication application);

    /** 중복 제출 유니크 위반을 서비스 계층에서 바로 처리할 수 있도록 변경을 DB에 반영한다. */
    void flush();

    /** 지망·답변까지 함께 읽는 상세 조회 */
    Optional<StaffApplication> findDetailById(Integer applicationId);

    Optional<StaffApplication> findDetailByRecruitmentIdAndApplicantId(Integer recruitmentId, Long applicantUserId);

    boolean existsByRecruitmentIdAndApplicantId(Integer recruitmentId, Long applicantUserId);

    /** 회장단 목록 — 부서 필터는 1·2지망 모두, 검색은 제출 당시 이름만 대상으로 한다. */
    long countForAdmin(Integer recruitmentId, Integer departmentId, String search);

    List<StaffApplication> findForAdminWithOffset(Integer recruitmentId, Integer departmentId, String search,
                                                  int page, int size);

    List<StaffApplication> findForAdminWithCursor(Integer recruitmentId, Integer departmentId, String search,
                                                  Integer cursor, int size);
}
