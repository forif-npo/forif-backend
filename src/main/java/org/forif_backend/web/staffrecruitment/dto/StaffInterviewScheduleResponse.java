package org.forif_backend.web.staffrecruitment.dto;

import org.forif_backend.application.staffrecruitment.dto.StaffInterviewScheduleDetail;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 면접 일정 조율 응답.
 *
 * 칸마다 누가 가능한지는 내려주지 않는다. 회장단 가능 시간과 지원자 가능 시간을
 * 화면이 겹쳐 계산해야 회장단이 시간을 고칠 때마다 다시 조회하지 않는다.
 * 면접 길이는 plan.slot_minutes이고, 시각은 시작 분(자정 기준)으로만 준다.
 */
public record StaffInterviewScheduleResponse(
        StaffInterviewPlanResponse plan,
        List<SlotResponse> hostSlots,
        List<ApplicantResponse> applicants,
        OffsetDateTime confirmedAt
) {

    public record SlotResponse(LocalDate date, int startMinute) {
        static SlotResponse from(StaffInterviewScheduleDetail.Slot slot) {
            return slot == null ? null : new SlotResponse(slot.date(), slot.startMinute());
        }
    }

    public record DepartmentResponse(Integer departmentId, String name, int priority) {
    }

    public record ApplicantResponse(
            Integer applicationId,
            String applicantName,
            String applicantDepartment,
            List<DepartmentResponse> appliedDepartments,
            List<SlotResponse> availableSlots,
            SlotResponse assignedSlot
    ) {
    }

    public static StaffInterviewScheduleResponse from(StaffInterviewScheduleDetail detail) {
        return new StaffInterviewScheduleResponse(
                StaffInterviewPlanResponse.from(detail.plan()),
                detail.hostSlots().stream().map(SlotResponse::from).toList(),
                detail.applicants().stream()
                        .map(applicant -> new ApplicantResponse(
                                applicant.applicationId(),
                                applicant.applicantName(),
                                applicant.applicantDepartment(),
                                applicant.appliedDepartments().stream()
                                        .map(department -> new DepartmentResponse(
                                                department.departmentId(), department.name(), department.priority()))
                                        .toList(),
                                applicant.availableSlots().stream().map(SlotResponse::from).toList(),
                                SlotResponse.from(applicant.assignedSlot())))
                        .toList(),
                StaffRecruitmentResponse.toOffset(detail.confirmedAt()));
    }
}
