package org.forif_backend.application.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewPlanDetail;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewScheduleDetail;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewScheduleDetail.Applicant;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewScheduleDetail.Department;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewScheduleDetail.Slot;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 면접 일정 조율.
 *
 * 회장단이 면접을 볼 수 있는 시간을 표시하면, 화면은 칸마다 회장단과 지원자가 모두 되는
 * 지원자를 보여 준다. 누구를 어느 칸에 넣을지는 회장단이 정한다. 서버는 자동으로 배정하지 않는다.
 *
 * 면접은 한 타임에 한 명이다. 회차 안에서 칸마다 한 명, 지원자마다 한 칸만 둔다.
 * 시각을 바꾸는 작업은 모집이 마감된 뒤에만 한다. 접수 중에는 지원자가 계속 바뀐다.
 */
@Service
@RequiredArgsConstructor
public class StaffInterviewScheduleService {

    private final StaffRecruitmentRepository staffRecruitmentRepository;
    private final StaffInterviewPlanRepository staffInterviewPlanRepository;
    private final StaffApplicationRepository staffApplicationRepository;
    private final StaffInterviewScheduleRepository staffInterviewScheduleRepository;

    @Transactional(readOnly = true)
    public StaffInterviewScheduleDetail getSchedule(Integer recruitmentId) {
        requireRecruitment(recruitmentId);
        StaffInterviewPlan plan = requirePlan(recruitmentId);
        return detail(recruitmentId, plan);
    }

    /**
     * 회장단 가능 시간을 보낸 목록으로 맞춘다.
     * 그대로 남는 칸은 지우지 않아 유니크 제약에 걸리지 않는다.
     */
    @Transactional
    public StaffInterviewScheduleDetail replaceHostSlots(Integer recruitmentId, List<Slot> requested) {
        StaffRecruitment recruitment = requireClosed(recruitmentId);
        StaffInterviewPlan plan = requirePlan(recruitmentId);

        Set<Slot> wanted = new LinkedHashSet<>(requested == null ? List.of() : requested);
        wanted.forEach(slot -> requireCovered(plan, slot.date(), slot.startMinute()));

        List<StaffInterviewHostSlot> current = staffInterviewScheduleRepository.findHostSlots(recruitmentId);
        List<StaffInterviewHostSlot> removed = current.stream()
                .filter(slot -> !wanted.contains(new Slot(slot.getSlotDate(), slot.getStartMinute())))
                .toList();
        Set<Slot> kept = current.stream()
                .map(slot -> new Slot(slot.getSlotDate(), slot.getStartMinute()))
                .collect(Collectors.toSet());
        List<StaffInterviewHostSlot> added = wanted.stream()
                .filter(slot -> !kept.contains(slot))
                .map(slot -> StaffInterviewHostSlot.create(recruitment, slot.date(), slot.startMinute()))
                .toList();

        staffInterviewScheduleRepository.deleteHostSlots(removed);
        staffInterviewScheduleRepository.saveHostSlots(added);
        staffInterviewScheduleRepository.flush();
        return detail(recruitmentId, plan);
    }

    /**
     * 지원자를 한 칸에 넣는다. 이미 넣었으면 그 칸으로 옮긴다.
     *
     * 회장단·지원자 가능 시간과 겹치지 않는 칸에도 넣을 수 있다.
     * 시간이 맞지 않는 지원자는 회장단이 따로 연락해 수동으로 정하기 때문이다.
     */
    @Transactional
    public StaffInterviewScheduleDetail assign(Integer recruitmentId, Integer applicationId,
                                               LocalDate date, int startMinute) {
        StaffRecruitment recruitment = requireClosed(recruitmentId);
        StaffInterviewPlan plan = requirePlan(recruitmentId);
        requireCovered(plan, date, startMinute);
        StaffApplication application = requireApplication(recruitmentId, applicationId);

        boolean takenByOther = staffInterviewScheduleRepository.findAssignments(recruitmentId).stream()
                .anyMatch(other -> other.isAt(date, startMinute)
                        && !other.getApplication().getId().equals(applicationId));
        if (takenByOther) {
            throw new ForifException(ErrorCode.STAFF_INTERVIEW_SLOT_TAKEN);
        }

        staffInterviewScheduleRepository.findAssignmentByApplicationId(applicationId)
                .ifPresentOrElse(
                        assignment -> assignment.moveTo(date, startMinute),
                        () -> staffInterviewScheduleRepository.saveAssignment(
                                StaffInterviewAssignment.create(recruitment, application, date, startMinute)));
        try {
            staffInterviewScheduleRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 두 회장단이 같은 칸에 동시에 넣으면 uk_staff_interview_assignment_slot에서 걸린다.
            throw new ForifException(ErrorCode.STAFF_INTERVIEW_SLOT_TAKEN);
        }
        return detail(recruitmentId, plan);
    }

    @Transactional
    public StaffInterviewScheduleDetail unassign(Integer recruitmentId, Integer applicationId) {
        requireClosed(recruitmentId);
        StaffInterviewPlan plan = requirePlan(recruitmentId);
        requireApplication(recruitmentId, applicationId);

        staffInterviewScheduleRepository.findAssignmentByApplicationId(applicationId)
                .ifPresent(staffInterviewScheduleRepository::deleteAssignment);
        staffInterviewScheduleRepository.flush();
        return detail(recruitmentId, plan);
    }

    /**
     * 면접 일정을 확정한다. 배정하지 못한 지원자가 있어도 확정할 수 있다.
     * 그 지원자는 면접 일시가 비어 있는 채로 넘어가고, 회장단이 나중에 정한다.
     */
    @Transactional
    public StaffInterviewScheduleDetail confirm(Integer recruitmentId) {
        requireClosed(recruitmentId);
        StaffInterviewPlan plan = requirePlan(recruitmentId);
        plan.confirmSchedule(LocalDateTime.now());
        staffInterviewPlanRepository.flush();
        return detail(recruitmentId, plan);
    }

    private StaffInterviewScheduleDetail detail(Integer recruitmentId, StaffInterviewPlan plan) {
        List<Slot> hostSlots = staffInterviewScheduleRepository.findHostSlots(recruitmentId).stream()
                .map(slot -> new Slot(slot.getSlotDate(), slot.getStartMinute()))
                .toList();

        Map<Integer, List<Slot>> availableByApplication = staffApplicationRepository
                .findInterviewSlotsByRecruitmentId(recruitmentId).stream()
                .collect(Collectors.groupingBy(
                        slot -> slot.getApplication().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(slot -> new Slot(slot.getSlotDate(), slot.getStartMinute()),
                                Collectors.toList())));

        Map<Integer, Slot> assignedByApplication = staffInterviewScheduleRepository
                .findAssignments(recruitmentId).stream()
                .collect(Collectors.toMap(
                        assignment -> assignment.getApplication().getId(),
                        assignment -> new Slot(assignment.getSlotDate(), assignment.getStartMinute())));

        List<Applicant> applicants = staffApplicationRepository
                .findAllWithChoicesByRecruitmentId(recruitmentId).stream()
                .map(application -> new Applicant(
                        application.getId(),
                        application.getApplicantName(),
                        application.getApplicantDepartment(),
                        application.getChoices().stream()
                                .sorted(Comparator.comparingInt(StaffApplicationChoice::getPriority))
                                .map(choice -> new Department(
                                        choice.getDepartment().getId(),
                                        choice.getDepartment().getName(),
                                        choice.getPriority()))
                                .toList(),
                        availableByApplication.getOrDefault(application.getId(), List.of()),
                        assignedByApplication.get(application.getId())))
                .toList();

        return new StaffInterviewScheduleDetail(
                StaffInterviewPlanDetail.from(plan), hostSlots, applicants, plan.getScheduleConfirmedAt());
    }

    private StaffRecruitment requireRecruitment(Integer recruitmentId) {
        return staffRecruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND));
    }

    /** 접수 중에는 지원자가 계속 바뀌므로 시각을 정하는 작업은 마감 뒤에만 한다. */
    private StaffRecruitment requireClosed(Integer recruitmentId) {
        StaffRecruitment recruitment = requireRecruitment(recruitmentId);
        if (recruitment.getStatus() != StaffRecruitmentStatus.CLOSED) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
        }
        return recruitment;
    }

    private StaffInterviewPlan requirePlan(Integer recruitmentId) {
        return staffInterviewPlanRepository.findByRecruitmentId(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_INTERVIEW_PLAN_NOT_FOUND));
    }

    private StaffApplication requireApplication(Integer recruitmentId, Integer applicationId) {
        return staffApplicationRepository.findDetailById(applicationId)
                .filter(application -> application.getRecruitment().getId().equals(recruitmentId))
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_APPLICATION_NOT_FOUND));
    }

    private void requireCovered(StaffInterviewPlan plan, LocalDate date, int startMinute) {
        if (date == null || !plan.covers(date, startMinute)) {
            throw new ForifException(ErrorCode.STAFF_INTERVIEW_SLOT_INVALID);
        }
    }
}
