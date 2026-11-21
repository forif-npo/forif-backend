package org.forif_backend.application.staffrecruitment.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 면접 일정 조율 화면 한 장에 필요한 모든 것.
 *
 * 칸마다 누가 가능한지는 화면이 회장단 가능 시간과 지원자 가능 시간을 겹쳐 계산한다.
 * 서버가 계산해 내려주면 회장단이 시간을 고칠 때마다 다시 조회해야 한다.
 */
public record StaffInterviewScheduleDetail(
        StaffInterviewPlanDetail plan,
        List<Slot> hostSlots,
        List<Applicant> applicants,
        LocalDateTime confirmedAt
) {

    public record Slot(LocalDate date, int startMinute) {
    }

    public record Department(Integer departmentId, String name, int priority) {
    }

    public record Applicant(
            Integer applicationId,
            String applicantName,
            String applicantDepartment,
            List<Department> appliedDepartments,
            List<Slot> availableSlots,
            Slot assignedSlot
    ) {
    }
}
