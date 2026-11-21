package org.forif_backend.domain.staffrecruitment;

import java.util.Optional;

public interface StaffInterviewPlanRepository {

    StaffInterviewPlan save(StaffInterviewPlan plan);

    /** 유니크 제약 위반을 서비스 계층에서 바로 처리할 수 있도록 변경을 DB에 반영한다. */
    void flush();

    /** 후보 날짜까지 함께 읽는다 (N+1 방지) */
    Optional<StaffInterviewPlan> findByRecruitmentId(Integer recruitmentId);

    void delete(StaffInterviewPlan plan);
}
