package org.forif_backend.application.staffrecruitment;

import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.StaffInterviewPlan;
import org.forif_backend.domain.staffrecruitment.StaffInterviewPlanRepository;
import org.forif_backend.domain.staffrecruitment.StaffInterviewScheduleRepository;
import org.forif_backend.domain.staffrecruitment.StaffInterviewAssignment;
import org.forif_backend.domain.staffrecruitment.StaffInterviewHostSlot;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffInterviewPlanServiceTest {

    private static final int RECRUITMENT_ID = 10;
    private static final LocalDate FIRST_DATE = LocalDate.of(2026, 12, 1);
    private static final LocalDate SECOND_DATE = LocalDate.of(2026, 12, 2);

    @Mock
    private StaffInterviewPlanRepository planRepository;

    @Mock
    private StaffRecruitmentRepository recruitmentRepository;

    @Mock
    private StaffInterviewScheduleRepository scheduleRepository;

    @InjectMocks
    private StaffInterviewPlanService service;

    private StaffRecruitment recruitment(boolean published) {
        StaffRecruitment recruitment = StaffRecruitment.create(2026, 2, "운영진 모집", null, null,
                LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 12, 1, 0, 0));
        if (published) {
            recruitment.publish();
        }
        ReflectionTestUtils.setField(recruitment, "id", RECRUITMENT_ID);
        return recruitment;
    }

    private StaffInterviewPlan existingPlan(StaffRecruitment recruitment) {
        StaffInterviewPlan plan = StaffInterviewPlan.create(recruitment, 9, 22, 30, true, null,
                List.of(FIRST_DATE, SECOND_DATE));
        ReflectionTestUtils.setField(plan, "version", 2L);
        return plan;
    }

    @Test
    void 공개_조회는_초안의_면접_일정을_숨긴다() {
        when(recruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment(false)));

        assertThatThrownBy(() -> service.findPublicByRecruitmentId(RECRUITMENT_ID))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_NOT_FOUND);
        verifyNoInteractions(planRepository);
    }

    @Test
    void 공개된_회차에_일정이_없으면_빈_값을_돌려준다() {
        when(recruitmentRepository.findById(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment(true)));
        when(planRepository.findByRecruitmentId(RECRUITMENT_ID)).thenReturn(Optional.empty());

        assertThat(service.findPublicByRecruitmentId(RECRUITMENT_ID)).isEmpty();
    }

    @Test
    void 오래된_버전이나_버전_누락은_기존_설정을_덮어쓰지_못한다() {
        StaffRecruitment recruitment = recruitment(true);
        StaffInterviewPlan plan = existingPlan(recruitment);
        when(recruitmentRepository.findByIdForUpdate(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment));
        when(planRepository.findByRecruitmentId(RECRUITMENT_ID)).thenReturn(Optional.of(plan));

        for (Long expectedVersion : new Long[]{1L, null}) {
            assertThatThrownBy(() -> service.save(RECRUITMENT_ID, List.of(SECOND_DATE),
                    10, 20, 30, true, null, expectedVersion))
                    .isInstanceOf(ForifException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_INTEGRITY_VIOLATION);
        }
        assertThat(plan.candidateDates()).containsExactly(FIRST_DATE, SECOND_DATE);
        verify(planRepository, never()).incrementVersion(plan);
        verify(planRepository, never()).flush();
    }

    @Test
    void 현재_버전으로_수정하면_새_순서로_응답한다() {
        StaffRecruitment recruitment = recruitment(true);
        StaffInterviewPlan plan = existingPlan(recruitment);
        when(recruitmentRepository.findByIdForUpdate(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment));
        when(planRepository.findByRecruitmentId(RECRUITMENT_ID)).thenReturn(Optional.of(plan));

        var detail = service.save(RECRUITMENT_ID, List.of(SECOND_DATE, FIRST_DATE),
                9, 22, 30, true, null, 2L);

        assertThat(detail.dates()).containsExactly(SECOND_DATE, FIRST_DATE);
        verify(planRepository).incrementVersion(plan);
        verify(planRepository).flush();
    }

    @Test
    void 삭제된_설정의_오래된_버전으로_새_설정을_만들지_못한다() {
        when(recruitmentRepository.findByIdForUpdate(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment(true)));
        when(planRepository.findByRecruitmentId(RECRUITMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(RECRUITMENT_ID, List.of(FIRST_DATE),
                9, 22, 30, true, null, 2L))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_INTEGRITY_VIOLATION);
        verify(planRepository, never()).save(any());
    }

    @Test
    void 배정된_지원자가_있으면_면접_일정을_수정하거나_삭제할_수_없다() {
        StaffRecruitment recruitment = recruitment(true);
        StaffInterviewPlan plan = existingPlan(recruitment);
        when(recruitmentRepository.findByIdForUpdate(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment));
        when(planRepository.findByRecruitmentId(RECRUITMENT_ID)).thenReturn(Optional.of(plan));
        when(scheduleRepository.findAssignments(RECRUITMENT_ID))
                .thenReturn(List.of(mock(StaffInterviewAssignment.class)));

        assertThatThrownBy(() -> service.save(RECRUITMENT_ID, List.of(SECOND_DATE),
                9, 22, 30, true, null, 2L))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_PLAN_HAS_ASSIGNMENTS);
        assertThatThrownBy(() -> service.delete(RECRUITMENT_ID))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_PLAN_HAS_ASSIGNMENTS);
        verify(planRepository, never()).delete(plan);
        verify(planRepository, never()).incrementVersion(plan);
    }

    @Test
    void 계획을_바꾸면_새_범위_밖의_회장단_가능_시간을_지운다() {
        StaffRecruitment recruitment = recruitment(true);
        StaffInterviewPlan plan = existingPlan(recruitment);
        plan.confirmSchedule(LocalDateTime.now());
        StaffInterviewHostSlot invalid = StaffInterviewHostSlot.create(recruitment, FIRST_DATE, 9 * 60);
        StaffInterviewHostSlot valid = StaffInterviewHostSlot.create(recruitment, SECOND_DATE, 9 * 60);
        when(recruitmentRepository.findByIdForUpdate(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment));
        when(planRepository.findByRecruitmentId(RECRUITMENT_ID)).thenReturn(Optional.of(plan));
        when(scheduleRepository.findHostSlots(RECRUITMENT_ID)).thenReturn(List.of(invalid, valid));

        service.save(RECRUITMENT_ID, List.of(SECOND_DATE), 9, 22, 30, true, null, 2L);

        verify(scheduleRepository).deleteHostSlots(List.of(invalid));
        assertThat(plan.isScheduleConfirmed()).isFalse();
    }

    @Test
    void 계획을_삭제하면_회장단_가능_시간도_지운다() {
        StaffRecruitment recruitment = recruitment(true);
        StaffInterviewPlan plan = existingPlan(recruitment);
        StaffInterviewHostSlot slot = StaffInterviewHostSlot.create(recruitment, FIRST_DATE, 9 * 60);
        when(recruitmentRepository.findByIdForUpdate(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment));
        when(planRepository.findByRecruitmentId(RECRUITMENT_ID)).thenReturn(Optional.of(plan));
        when(scheduleRepository.findHostSlots(RECRUITMENT_ID)).thenReturn(List.of(slot));

        service.delete(RECRUITMENT_ID);

        verify(scheduleRepository).deleteHostSlots(List.of(slot));
        verify(planRepository).delete(plan);
    }
}
