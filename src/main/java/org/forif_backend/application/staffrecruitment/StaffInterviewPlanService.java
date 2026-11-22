package org.forif_backend.application.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewPlanDetail;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.StaffInterviewPlan;
import org.forif_backend.domain.staffrecruitment.StaffInterviewPlanRepository;
import org.forif_backend.domain.staffrecruitment.StaffInterviewScheduleRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 모집 회차의 면접 일정 설정.
 *
 * 회장단이 후보 날짜와 수집 시간 범위를 정하면 지원 화면에 시간표가 나타난다.
 * 설정하지 않은 회차에서는 면접 가능 시간을 받지 않는다.
 */
@Service
@RequiredArgsConstructor
public class StaffInterviewPlanService {

    private final StaffInterviewPlanRepository staffInterviewPlanRepository;
    private final StaffInterviewScheduleRepository staffInterviewScheduleRepository;
    private final StaffRecruitmentRepository staffRecruitmentRepository;

    /** 공개 지원 화면용. 설정이 없으면 비어 있다. */
    @Transactional(readOnly = true)
    public Optional<StaffInterviewPlanDetail> findByRecruitmentId(Integer recruitmentId) {
        return staffInterviewPlanRepository.findByRecruitmentId(recruitmentId)
                .map(StaffInterviewPlanDetail::from);
    }

    /** 공개 조회에서는 초안을 숨기고, 일정 미설정 회차는 빈 값으로 돌려준다. */
    @Transactional(readOnly = true)
    public Optional<StaffInterviewPlanDetail> findPublicByRecruitmentId(Integer recruitmentId) {
        StaffRecruitment recruitment = staffRecruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND));
        if (!recruitment.isPubliclyVisible()) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND);
        }
        return findByRecruitmentId(recruitmentId);
    }

    @Transactional(readOnly = true)
    public StaffInterviewPlanDetail getByRecruitmentId(Integer recruitmentId) {
        return findByRecruitmentId(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_INTERVIEW_PLAN_NOT_FOUND));
    }

    /**
     * 면접 일정을 설정한다. 이미 있으면 내용을 바꾼다.
     *
     * 이미 제출된 지원서의 선택은 날짜·시간 값으로 보관하므로 후보가 바뀌어도 지워지지 않는다.
     * 범위 밖이 된 선택은 집계 화면에서 회장단이 확인한다.
     */
    @Transactional
    public StaffInterviewPlanDetail save(Integer recruitmentId, List<LocalDate> dates,
                                         int startHour, int endHour, int slotMinutes,
                                         boolean required, String guide, Long expectedVersion) {
        StaffRecruitment recruitment = staffRecruitmentRepository.findByIdForUpdate(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND));

        StaffInterviewPlan plan = staffInterviewPlanRepository.findByRecruitmentId(recruitmentId)
                .map(existing -> {
                    if (expectedVersion == null || !expectedVersion.equals(existing.getVersion())) {
                        throw new ForifException(ErrorCode.DATA_INTEGRITY_VIOLATION);
                    }
                    requireNoAssignments(recruitmentId);
                    staffInterviewPlanRepository.incrementVersion(existing);
                    existing.update(startHour, endHour, slotMinutes, required, guide, dates);
                    return existing;
                })
                .orElseGet(() -> {
                    if (expectedVersion != null) {
                        throw new ForifException(ErrorCode.DATA_INTEGRITY_VIOLATION);
                    }
                    return staffInterviewPlanRepository.save(StaffInterviewPlan.create(
                            recruitment, startHour, endHour, slotMinutes, required, guide, dates));
                });

        var invalidHostSlots = staffInterviewScheduleRepository.findHostSlots(recruitmentId).stream()
                .filter(slot -> !plan.covers(slot.getSlotDate(), slot.getStartMinute()))
                .toList();
        staffInterviewScheduleRepository.deleteHostSlots(invalidHostSlots);

        // 화면은 이 응답의 version으로 다음 요청을 보낸다. flush 전에는 증가 전 값이 나가 바로 409가 된다.
        staffInterviewPlanRepository.flush();
        return StaffInterviewPlanDetail.from(plan);
    }

    /** 면접을 받지 않기로 한 회차의 설정을 지운다. 이미 제출된 선택은 남는다. */
    @Transactional
    public void delete(Integer recruitmentId) {
        staffRecruitmentRepository.findByIdForUpdate(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND));
        requireNoAssignments(recruitmentId);
        staffInterviewScheduleRepository.deleteHostSlots(
                staffInterviewScheduleRepository.findHostSlots(recruitmentId));
        staffInterviewPlanRepository.findByRecruitmentId(recruitmentId)
                .ifPresent(staffInterviewPlanRepository::delete);
    }

    private void requireNoAssignments(Integer recruitmentId) {
        if (!staffInterviewScheduleRepository.findAssignments(recruitmentId).isEmpty()) {
            throw new ForifException(ErrorCode.STAFF_INTERVIEW_PLAN_HAS_ASSIGNMENTS);
        }
    }
}
