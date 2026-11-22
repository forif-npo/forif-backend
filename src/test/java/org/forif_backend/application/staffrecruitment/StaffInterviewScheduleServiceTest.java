package org.forif_backend.application.staffrecruitment;

import org.forif_backend.application.staffrecruitment.dto.StaffInterviewScheduleDetail;
import org.forif_backend.application.staffrecruitment.dto.StaffInterviewScheduleDetail.Slot;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffInterviewScheduleServiceTest {

    private static final Integer RECRUITMENT_ID = 12;
    private static final Integer FIRST_APPLICATION = 101;
    private static final Integer SECOND_APPLICATION = 102;
    private static final LocalDate DEC_1 = LocalDate.of(2026, 12, 1);
    private static final int NINE = 9 * 60;
    private static final int NINE_THIRTY = 9 * 60 + 30;

    @Mock
    private StaffRecruitmentRepository staffRecruitmentRepository;

    @Mock
    private StaffInterviewPlanRepository staffInterviewPlanRepository;

    @Mock
    private StaffApplicationRepository staffApplicationRepository;

    @Mock
    private StaffInterviewScheduleRepository staffInterviewScheduleRepository;

    @InjectMocks
    private StaffInterviewScheduleService staffInterviewScheduleService;

    private StaffRecruitment recruitment(boolean closed) {
        LocalDateTime now = LocalDateTime.now();
        StaffRecruitment recruitment = StaffRecruitment.create(
                2026, 2, "2026-2 운영진 모집", null, null, now.minusDays(1), now.plusDays(1));
        recruitment.publish();
        ReflectionTestUtils.setField(recruitment, "id", RECRUITMENT_ID);
        if (closed) {
            recruitment.close("면접 준비", now);
        }
        return recruitment;
    }

    private StaffApplication application(StaffRecruitment recruitment, Integer id, String name) {
        // 접수 중에만 제출할 수 있어 마감 전 회차로 만든 뒤 회차를 바꿔 끼운다.
        StaffRecruitment open = recruitment(false);
        StaffApplication application = StaffApplication.submit(
                open, null, name, name + "@example.com", null, "컴퓨터소프트웨어학부", LocalDateTime.now());
        ReflectionTestUtils.setField(application, "recruitment", recruitment);
        ReflectionTestUtils.setField(application, "id", id);
        return application;
    }

    private StaffInterviewPlan plan(StaffRecruitment recruitment) {
        return StaffInterviewPlan.create(recruitment, 9, 22, 30, true, null, List.of(DEC_1));
    }

    private void given(StaffRecruitment recruitment, StaffInterviewPlan plan) {
        lenient().when(staffRecruitmentRepository.findById(RECRUITMENT_ID))
                .thenReturn(Optional.of(recruitment));
        lenient().when(staffRecruitmentRepository.findByIdForUpdate(RECRUITMENT_ID))
                .thenReturn(Optional.of(recruitment));
        when(staffInterviewPlanRepository.findByRecruitmentId(RECRUITMENT_ID)).thenReturn(Optional.of(plan));
        lenient().when(staffApplicationRepository.findInterviewSlotsByRecruitmentId(RECRUITMENT_ID))
                .thenReturn(List.of());
        lenient().when(staffApplicationRepository.findAllWithChoicesByRecruitmentId(RECRUITMENT_ID))
                .thenReturn(List.of());
        lenient().when(staffInterviewScheduleRepository.findHostSlots(RECRUITMENT_ID)).thenReturn(List.of());
    }

    @Test
    void 빈_칸에_지원자를_배정한다() {
        StaffRecruitment recruitment = recruitment(true);
        given(recruitment, plan(recruitment));
        StaffApplication first = application(recruitment, FIRST_APPLICATION, "가지원");
        when(staffApplicationRepository.findDetailById(FIRST_APPLICATION)).thenReturn(Optional.of(first));
        when(staffInterviewScheduleRepository.findAssignments(RECRUITMENT_ID)).thenReturn(List.of());
        when(staffInterviewScheduleRepository.findAssignmentByApplicationId(FIRST_APPLICATION))
                .thenReturn(Optional.empty());

        staffInterviewScheduleService.assign(RECRUITMENT_ID, FIRST_APPLICATION, DEC_1, NINE);

        ArgumentCaptor<StaffInterviewAssignment> saved = ArgumentCaptor.forClass(StaffInterviewAssignment.class);
        verify(staffInterviewScheduleRepository).saveAssignment(saved.capture());
        assertThat(saved.getValue().isAt(DEC_1, NINE)).isTrue();
        assertThat(saved.getValue().getApplication()).isSameAs(first);
    }

    @Test
    void 다른_지원자가_있는_칸에는_넣을_수_없다() {
        // 면접은 한 타임에 한 명이다.
        StaffRecruitment recruitment = recruitment(true);
        given(recruitment, plan(recruitment));
        StaffApplication first = application(recruitment, FIRST_APPLICATION, "가지원");
        StaffApplication second = application(recruitment, SECOND_APPLICATION, "나지원");
        when(staffApplicationRepository.findDetailById(SECOND_APPLICATION)).thenReturn(Optional.of(second));
        when(staffInterviewScheduleRepository.findAssignments(RECRUITMENT_ID)).thenReturn(List.of(
                StaffInterviewAssignment.create(recruitment, first, DEC_1, NINE)));

        assertThatThrownBy(() -> staffInterviewScheduleService.assign(
                RECRUITMENT_ID, SECOND_APPLICATION, DEC_1, NINE))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_SLOT_TAKEN);
        verify(staffInterviewScheduleRepository, never()).saveAssignment(any());
    }

    @Test
    void 이미_배정한_지원자는_새_칸으로_옮긴다() {
        // 면접이 길어지거나 바뀌면 회장단이 수정한다.
        StaffRecruitment recruitment = recruitment(true);
        given(recruitment, plan(recruitment));
        StaffApplication first = application(recruitment, FIRST_APPLICATION, "가지원");
        StaffInterviewAssignment current = StaffInterviewAssignment.create(recruitment, first, DEC_1, NINE);
        when(staffApplicationRepository.findDetailById(FIRST_APPLICATION)).thenReturn(Optional.of(first));
        when(staffInterviewScheduleRepository.findAssignments(RECRUITMENT_ID)).thenReturn(List.of(current));
        when(staffInterviewScheduleRepository.findAssignmentByApplicationId(FIRST_APPLICATION))
                .thenReturn(Optional.of(current));

        staffInterviewScheduleService.assign(RECRUITMENT_ID, FIRST_APPLICATION, DEC_1, NINE_THIRTY);

        assertThat(current.isAt(DEC_1, NINE_THIRTY)).isTrue();
        verify(staffInterviewScheduleRepository, never()).saveAssignment(any());
    }

    @Test
    void 면접_일정_범위_밖이나_칸_경계가_아닌_시각은_거절한다() {
        StaffRecruitment recruitment = recruitment(true);
        given(recruitment, plan(recruitment));

        List.of(22 * 60, 9 * 60 + 10).forEach(minute ->
                assertThatThrownBy(() -> staffInterviewScheduleService.assign(
                        RECRUITMENT_ID, FIRST_APPLICATION, DEC_1, minute))
                        .isInstanceOf(ForifException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_SLOT_INVALID));
    }

    @Test
    void 마감_전에는_시각을_정할_수_없다() {
        // 접수 중에는 지원자가 계속 바뀐다.
        StaffRecruitment recruitment = recruitment(false);
        when(staffRecruitmentRepository.findByIdForUpdate(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment));

        assertThatThrownBy(() -> staffInterviewScheduleService.assign(
                RECRUITMENT_ID, FIRST_APPLICATION, DEC_1, NINE))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
    }

    @Test
    void 다른_회차의_지원서는_배정할_수_없다() {
        StaffRecruitment recruitment = recruitment(true);
        given(recruitment, plan(recruitment));
        StaffRecruitment other = recruitment(true);
        ReflectionTestUtils.setField(other, "id", 99);
        StaffApplication foreign = application(other, FIRST_APPLICATION, "가지원");
        when(staffApplicationRepository.findDetailById(FIRST_APPLICATION)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> staffInterviewScheduleService.assign(
                RECRUITMENT_ID, FIRST_APPLICATION, DEC_1, NINE))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_NOT_FOUND);
    }

    @Test
    void 회장단_가능_시간은_남는_칸을_두고_차이만_반영한다() {
        StaffRecruitment recruitment = recruitment(true);
        given(recruitment, plan(recruitment));
        StaffInterviewHostSlot kept = StaffInterviewHostSlot.create(recruitment, DEC_1, NINE);
        StaffInterviewHostSlot dropped = StaffInterviewHostSlot.create(recruitment, DEC_1, 10 * 60);
        when(staffInterviewScheduleRepository.findHostSlots(RECRUITMENT_ID)).thenReturn(List.of(kept, dropped));

        staffInterviewScheduleService.replaceHostSlots(RECRUITMENT_ID, List.of(
                new Slot(DEC_1, NINE), new Slot(DEC_1, NINE_THIRTY)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StaffInterviewHostSlot>> removed = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StaffInterviewHostSlot>> added = ArgumentCaptor.forClass(List.class);
        verify(staffInterviewScheduleRepository).deleteHostSlots(removed.capture());
        verify(staffInterviewScheduleRepository).saveHostSlots(added.capture());
        assertThat(removed.getValue()).containsExactly(dropped);
        assertThat(added.getValue()).hasSize(1);
        assertThat(added.getValue().get(0).isAt(DEC_1, NINE_THIRTY)).isTrue();
    }

    @Test
    void 회장단_가능_시간도_면접_일정_범위_안이어야_한다() {
        StaffRecruitment recruitment = recruitment(true);
        given(recruitment, plan(recruitment));

        assertThatThrownBy(() -> staffInterviewScheduleService.replaceHostSlots(
                RECRUITMENT_ID, List.of(new Slot(LocalDate.of(2026, 12, 5), NINE))))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_SLOT_INVALID);
        verify(staffInterviewScheduleRepository, never()).saveHostSlots(anyList());
    }

    @Test
    void 일정을_확정하면_확정_시각을_남긴다() {
        StaffRecruitment recruitment = recruitment(true);
        StaffInterviewPlan plan = plan(recruitment);
        given(recruitment, plan);
        when(staffInterviewScheduleRepository.findAssignments(RECRUITMENT_ID)).thenReturn(List.of());

        StaffInterviewScheduleDetail detail = staffInterviewScheduleService.confirm(RECRUITMENT_ID);

        assertThat(plan.isScheduleConfirmed()).isTrue();
        assertThat(detail.confirmedAt()).isNotNull();
    }

    @Test
    void 조회는_지원자마다_가능_시간과_배정_시각을_묶어_준다() {
        StaffRecruitment recruitment = recruitment(true);
        given(recruitment, plan(recruitment));
        StaffApplication first = application(recruitment, FIRST_APPLICATION, "가지원");
        StaffApplication second = application(recruitment, SECOND_APPLICATION, "나지원");
        StaffApplicationInterviewSlot firstNine = StaffApplicationInterviewSlot.create(DEC_1, NINE);
        StaffApplicationInterviewSlot firstNineThirty = StaffApplicationInterviewSlot.create(DEC_1, NINE_THIRTY);
        first.addInterviewSlot(firstNine);
        first.addInterviewSlot(firstNineThirty);
        when(staffApplicationRepository.findAllWithChoicesByRecruitmentId(RECRUITMENT_ID))
                .thenReturn(List.of(first, second));
        when(staffApplicationRepository.findInterviewSlotsByRecruitmentId(RECRUITMENT_ID))
                .thenReturn(List.of(firstNine, firstNineThirty));
        when(staffInterviewScheduleRepository.findAssignments(RECRUITMENT_ID)).thenReturn(List.of(
                StaffInterviewAssignment.create(recruitment, first, DEC_1, NINE_THIRTY)));

        StaffInterviewScheduleDetail detail = staffInterviewScheduleService.getSchedule(RECRUITMENT_ID);

        assertThat(detail.applicants()).hasSize(2);
        assertThat(detail.applicants().get(0).availableSlots())
                .containsExactly(new Slot(DEC_1, NINE), new Slot(DEC_1, NINE_THIRTY));
        assertThat(detail.applicants().get(0).assignedSlot()).isEqualTo(new Slot(DEC_1, NINE_THIRTY));
        // 가능 시간을 하나도 안 낸 지원자도 빠지지 않는다. 회장단이 수동으로 정한다.
        assertThat(detail.applicants().get(1).availableSlots()).isEmpty();
        assertThat(detail.applicants().get(1).assignedSlot()).isNull();
    }
}
