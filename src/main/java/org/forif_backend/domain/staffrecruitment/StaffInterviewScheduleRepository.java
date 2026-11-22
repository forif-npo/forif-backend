package org.forif_backend.domain.staffrecruitment;

import java.util.List;
import java.util.Optional;

/**
 * 면접 일정 조율 저장소. 회장단 가능 시간과 지원자별 면접 시각을 함께 다룬다.
 */
public interface StaffInterviewScheduleRepository {

    List<StaffInterviewHostSlot> findHostSlots(Integer recruitmentId);

    void saveHostSlots(List<StaffInterviewHostSlot> slots);

    void deleteHostSlots(List<StaffInterviewHostSlot> slots);

    List<StaffInterviewAssignment> findAssignments(Integer recruitmentId);

    Optional<StaffInterviewAssignment> findAssignmentByApplicationId(Integer applicationId);

    StaffInterviewAssignment saveAssignment(StaffInterviewAssignment assignment);

    void deleteAssignment(StaffInterviewAssignment assignment);

    /** 유니크 제약 위반을 서비스 계층에서 바로 처리할 수 있도록 변경을 DB에 반영한다. */
    void flush();
}
