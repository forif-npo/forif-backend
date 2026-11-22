package org.forif_backend.domain.staffrecruitment;

import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 면접 일정의 시간 범위, 후보 날짜 병합, 지원자 선택이 범위 안인지 판단하는 규칙.
 */
class StaffInterviewPlanTest {

    private static final LocalDate DEC_1 = LocalDate.of(2026, 12, 1);
    private static final LocalDate DEC_2 = LocalDate.of(2026, 12, 2);
    private static final LocalDate DEC_3 = LocalDate.of(2026, 12, 3);
    private static final LocalDate DEC_4 = LocalDate.of(2026, 12, 4);

    private StaffRecruitment recruitment() {
        return StaffRecruitment.create(2026, 2, "2026-2 운영진 모집", null, null,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(7));
    }

    private StaffInterviewPlan plan() {
        return StaffInterviewPlan.create(recruitment(), 9, 22, 30, true,
                "면접 가능 시간을 표시해주세요.", List.of(DEC_1, DEC_2, DEC_3, DEC_4));
    }

    @Test
    void 후보_날짜와_시간_범위를_보관한다() {
        StaffInterviewPlan plan = plan();

        assertThat(plan.candidateDates()).containsExactly(DEC_1, DEC_2, DEC_3, DEC_4);
        assertThat(plan.getStartHour()).isEqualTo(9);
        assertThat(plan.getEndHour()).isEqualTo(22);
        assertThat(plan.getSlotMinutes()).isEqualTo(30);
        assertThat(plan.isRequired()).isTrue();
    }

    @Test
    void 하루_칸_수는_시간_범위를_칸_길이로_나눈_값이다() {
        // 9~22시를 30분으로 나누면 26칸이다.
        assertThat(plan().slotsPerDay()).isEqualTo(26);
    }

    @Test
    void 칸_길이를_바꾸면_하루_칸_수도_바뀐다() {
        assertThat(StaffInterviewPlan.create(recruitment(), 9, 22, 60, true, null, List.of(DEC_1))
                .slotsPerDay()).isEqualTo(13);
        assertThat(StaffInterviewPlan.create(recruitment(), 9, 22, 15, true, null, List.of(DEC_1))
                .slotsPerDay()).isEqualTo(52);
    }

    @Test
    void 허용하지_않는_칸_길이는_거절한다() {
        List.of(7, 45, 0, 90).forEach(minutes ->
                assertThatThrownBy(() -> StaffInterviewPlan.create(
                        recruitment(), 9, 22, minutes, true, null, List.of(DEC_1)))
                        .isInstanceOf(ForifException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_PLAN_INVALID_INPUT));
    }

    @Test
    void 범위_안이면서_칸_경계에_맞는_시각만_인정한다() {
        StaffInterviewPlan plan = plan();

        assertThat(plan.covers(DEC_1, 9 * 60)).isTrue();
        assertThat(plan.covers(DEC_1, 9 * 60 + 30)).isTrue();
        assertThat(plan.covers(DEC_4, 21 * 60 + 30)).isTrue();
        // 30분 단위에서 10분 시작은 칸 경계가 아니다.
        assertThat(plan.covers(DEC_1, 9 * 60 + 10)).isFalse();
        // end_hour는 마지막 칸의 끝 시각이라 22:00에 시작하는 칸은 없다.
        assertThat(plan.covers(DEC_1, 22 * 60)).isFalse();
        assertThat(plan.covers(DEC_1, 8 * 60)).isFalse();
        assertThat(plan.covers(LocalDate.of(2026, 12, 5), 10 * 60)).isFalse();
    }

    @Test
    void 시작이_끝보다_늦거나_같으면_거절한다() {
        List.of(List.of(22, 9), List.of(9, 9)).forEach(hours ->
                assertThatThrownBy(() -> StaffInterviewPlan.create(
                        recruitment(), hours.get(0), hours.get(1), 30, true, null, List.of(DEC_1)))
                        .isInstanceOf(ForifException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_PLAN_INVALID_INPUT));
    }

    @Test
    void 후보_날짜가_없으면_거절한다() {
        assertThatThrownBy(() -> StaffInterviewPlan.create(recruitment(), 9, 22, 30, true, null, List.of()))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_PLAN_INVALID_INPUT);
    }

    @Test
    void 후보_날짜는_열네_개까지다() {
        List<LocalDate> tooMany = IntStream.rangeClosed(1, 15).mapToObj(DEC_1::plusDays).toList();

        assertThatThrownBy(() -> StaffInterviewPlan.create(recruitment(), 9, 22, 30, true, null, tooMany))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_PLAN_INVALID_INPUT);
    }

    @Test
    void 같은_날짜를_다시_보내면_그_행을_그대로_둔다() {
        // 전체 삭제 후 재삽입하면 uk_staff_interview_plan_date에 걸린다.
        StaffInterviewPlan plan = plan();
        StaffInterviewPlanDate kept = plan.getDates().get(0);

        plan.update(10, 20, 60, false, "바뀐 안내", List.of(DEC_1, DEC_2));

        assertThat(plan.getDates()).hasSize(2);
        assertThat(plan.getDates().get(0)).isSameAs(kept);
        assertThat(plan.candidateDates()).containsExactly(DEC_1, DEC_2);
        assertThat(plan.getStartHour()).isEqualTo(10);
        assertThat(plan.isRequired()).isFalse();
    }

    @Test
    void 기존_날짜의_순서를_바꾸면_저장_직후에도_새_순서로_응답한다() {
        StaffInterviewPlan plan = plan();
        StaffInterviewPlanDate kept = plan.getDates().get(0);

        plan.update(9, 22, 30, true, null, List.of(DEC_3, DEC_1, DEC_2));

        assertThat(plan.getDates()).contains(kept);
        assertThat(plan.candidateDates()).containsExactly(DEC_3, DEC_1, DEC_2);
    }

    @Test
    void 중복된_날짜를_보내면_한_번만_담는다() {
        StaffInterviewPlan plan = StaffInterviewPlan.create(
                recruitment(), 9, 22, 30, true, null, List.of(DEC_1, DEC_1, DEC_2));

        assertThat(plan.candidateDates()).containsExactly(DEC_1, DEC_2);
    }
}
