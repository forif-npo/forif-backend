package org.forif_backend.domain.staffrecruitment;

import java.util.Optional;

public interface StaffInterviewPlanRepository {

    StaffInterviewPlan save(StaffInterviewPlan plan);

    /** 변경된 버전을 응답에 담을 수 있도록 DB에 반영한다. */
    void flush();

    /** 날짜 행만 바뀌어도 일정 버전이 증가하도록 부모 행을 잠그고 버전을 올린다. */
    void incrementVersion(StaffInterviewPlan plan);

    /** 후보 날짜까지 함께 읽는다 (N+1 방지) */
    Optional<StaffInterviewPlan> findByRecruitmentId(Integer recruitmentId);

    void delete(StaffInterviewPlan plan);
}
