package org.forif_backend.domain.staffrecruitment;

import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 모집 회차의 상태 전이와 접수 기간 경계.
 * 접수 가능 여부를 화면·API 어디서도 다시 계산하지 않도록 이 규칙만 검증한다.
 */
class StaffRecruitmentTest {

    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2026, 12, 1, 0, 0);
    private static final LocalDateTime ENDS_AT = LocalDateTime.of(2026, 12, 15, 18, 0);

    private StaffRecruitment published() {
        StaffRecruitment recruitment = draft();
        recruitment.publish();
        return recruitment;
    }

    private StaffRecruitment draft() {
        return StaffRecruitment.create(2027, 1, "2027-1 운영진 모집", "요약", "안내", STARTS_AT, ENDS_AT);
    }

    @Test
    void 시작_시각_직전에는_접수가_열리지_않는다() {
        StaffRecruitment recruitment = published();

        assertThat(recruitment.applicationStatus(STARTS_AT.minusSeconds(1)))
                .isEqualTo(RecruitmentApplicationStatus.UPCOMING);
        assertThat(recruitment.isApplicationOpen(STARTS_AT.minusSeconds(1))).isFalse();
    }

    @Test
    void 시작_시각에는_접수가_열린다() {
        StaffRecruitment recruitment = published();

        assertThat(recruitment.applicationStatus(STARTS_AT)).isEqualTo(RecruitmentApplicationStatus.OPEN);
        assertThat(recruitment.isApplicationOpen(ENDS_AT.minusSeconds(1))).isTrue();
    }

    @Test
    void 종료_시각에는_접수가_닫힌다() {
        StaffRecruitment recruitment = published();

        assertThat(recruitment.applicationStatus(ENDS_AT)).isEqualTo(RecruitmentApplicationStatus.CLOSED);
        assertThat(recruitment.isApplicationOpen(ENDS_AT)).isFalse();
    }

    @Test
    void 게시하지_않은_회차는_기간_안이어도_접수가_닫혀_있다() {
        StaffRecruitment recruitment = draft();

        assertThat(recruitment.applicationStatus(STARTS_AT)).isEqualTo(RecruitmentApplicationStatus.CLOSED);
        assertThat(recruitment.isPubliclyVisible()).isFalse();
    }

    @Test
    void 기간이_없으면_게시할_수_없다() {
        StaffRecruitment recruitment = StaffRecruitment.create(2027, 1, "제목", null, null, null, null);

        assertThatThrownBy(recruitment::publish)
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_INVALID_PERIOD);
    }

    @Test
    void 종료가_시작보다_빠르면_만들_수_없다() {
        assertThatThrownBy(() -> StaffRecruitment.create(2027, 1, "제목", null, null, ENDS_AT, STARTS_AT))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_INVALID_PERIOD);
    }

    @Test
    void 초안이_아니면_게시할_수_없다() {
        StaffRecruitment recruitment = published();

        assertThatThrownBy(recruitment::publish)
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
    }

    @Test
    void 종료_시각_이후의_마감은_자연_종료로_기록한다() {
        StaffRecruitment recruitment = published();

        recruitment.close("기간 종료", ENDS_AT.plusHours(3));

        assertThat(recruitment.getStatus()).isEqualTo(StaffRecruitmentStatus.CLOSED);
        assertThat(recruitment.getEndedAt()).isEqualTo(ENDS_AT);
        assertThat(recruitment.getPublishedSlot()).isNull();
    }

    @Test
    void 조기_마감은_처리_시각을_기록한다() {
        StaffRecruitment recruitment = published();
        LocalDateTime closedAt = STARTS_AT.plusDays(1);

        recruitment.close("정원 충족", closedAt);

        assertThat(recruitment.getEndedAt()).isEqualTo(closedAt);
        assertThat(recruitment.getCloseReason()).isEqualTo("정원 충족");
        assertThat(recruitment.applicationStatus(STARTS_AT.plusDays(2)))
                .isEqualTo(RecruitmentApplicationStatus.CLOSED);
    }

    @Test
    void 취소한_회차는_다시_바꿀_수_없다() {
        StaffRecruitment recruitment = published();
        recruitment.cancel("모집 철회", STARTS_AT.plusDays(1));

        assertThat(recruitment.applicationStatus(STARTS_AT.plusDays(2)))
                .isEqualTo(RecruitmentApplicationStatus.CANCELED);
        assertThatThrownBy(() -> recruitment.cancel("다시 취소", STARTS_AT.plusDays(3)))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
        assertThatThrownBy(() -> recruitment.updateDetails("새 제목", null, null, STARTS_AT, ENDS_AT))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
    }

    @Test
    void 부서와_문항은_보낸_목록으로_맞춰진다() {
        StaffRecruitment recruitment = draft();

        recruitment.mergeDepartments(List.of(
                new StaffRecruitmentDepartmentSpec(null, "기획팀", null, 1, true),
                new StaffRecruitmentDepartmentSpec(null, "개발팀", null, 2, true)), false);
        recruitment.mergeQuestions(List.of(new StaffRecruitmentQuestionSpec(
                null, "지원 동기", null, StaffRecruitmentQuestionType.LONG_TEXT, true, 1000, 1)), false);

        assertThat(recruitment.getDepartments()).hasSize(2);
        assertThat(recruitment.getDepartments().get(0).getRecruitment()).isSameAs(recruitment);
        assertThat(recruitment.getQuestions()).hasSize(1);
    }

    @Test
    void 같은_이름을_유지한_수정은_식별자로_같은_부서를_고친다() {
        StaffRecruitment recruitment = withDepartments("기획팀", "개발팀");
        Integer planningId = recruitment.getDepartments().get(0).getId();

        recruitment.mergeDepartments(List.of(
                new StaffRecruitmentDepartmentSpec(planningId, "기획팀", "설명을 고쳤다", 1, true),
                new StaffRecruitmentDepartmentSpec(recruitment.getDepartments().get(1).getId(), "개발팀", null, 2, true)),
                false);

        assertThat(recruitment.getDepartments()).hasSize(2);
        assertThat(recruitment.getDepartments().get(0).getId()).isEqualTo(planningId);
        assertThat(recruitment.getDepartments().get(0).getDescription()).isEqualTo("설명을 고쳤다");
    }

    @Test
    void 목록에서_빠진_부서는_지워진다() {
        StaffRecruitment recruitment = withDepartments("기획팀", "개발팀");
        Integer devId = recruitment.getDepartments().get(1).getId();

        recruitment.mergeDepartments(
                List.of(new StaffRecruitmentDepartmentSpec(devId, "개발팀", null, 1, true)), false);

        assertThat(recruitment.getDepartments()).hasSize(1);
        assertThat(recruitment.getDepartments().get(0).getName()).isEqualTo("개발팀");
    }

    @Test
    void 제출된_지원서가_있으면_부서를_지울_수_없다() {
        StaffRecruitment recruitment = withDepartments("기획팀", "개발팀");
        Integer devId = recruitment.getDepartments().get(1).getId();

        assertThatThrownBy(() -> recruitment.mergeDepartments(
                List.of(new StaffRecruitmentDepartmentSpec(devId, "개발팀", null, 1, true)), true))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_STRUCTURE_LOCKED);

        assertThat(recruitment.getDepartments()).hasSize(2);
    }

    @Test
    void 제출된_지원서가_있어도_내용_수정과_숨김은_된다() {
        StaffRecruitment recruitment = withDepartments("기획팀", "개발팀");

        recruitment.mergeDepartments(List.of(
                new StaffRecruitmentDepartmentSpec(
                        recruitment.getDepartments().get(0).getId(), "기획팀", "설명", 1, false),
                new StaffRecruitmentDepartmentSpec(
                        recruitment.getDepartments().get(1).getId(), "개발팀", null, 2, true)), true);

        assertThat(recruitment.getDepartments().get(0).isActive()).isFalse();
        assertThat(recruitment.getDepartments().get(1).isActive()).isTrue();
    }

    @Test
    void 없는_부서_식별자를_보내면_거절한다() {
        StaffRecruitment recruitment = withDepartments("기획팀");

        assertThatThrownBy(() -> recruitment.mergeDepartments(
                List.of(new StaffRecruitmentDepartmentSpec(9999, "기획팀", null, 1, true)), false))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 한_요청에_같은_부서_이름이_두_번_오면_거절한다() {
        StaffRecruitment recruitment = draft();

        assertThatThrownBy(() -> recruitment.mergeDepartments(List.of(
                new StaffRecruitmentDepartmentSpec(null, "기획팀", null, 1, true),
                new StaffRecruitmentDepartmentSpec(null, "기획팀", null, 2, true)), false))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 지울_부서와_같은_이름으로_새로_추가하면_거절한다() {
        StaffRecruitment recruitment = withDepartments("기획팀");

        assertThatThrownBy(() -> recruitment.mergeDepartments(
                List.of(new StaffRecruitmentDepartmentSpec(null, "기획팀", null, 1, true)), false))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 제출된_답변이_있으면_문항을_지울_수_없다() {
        StaffRecruitment recruitment = draft();
        recruitment.mergeQuestions(List.of(
                new StaffRecruitmentQuestionSpec(null, "지원 동기", null,
                        StaffRecruitmentQuestionType.LONG_TEXT, true, 1000, 1),
                new StaffRecruitmentQuestionSpec(null, "한 줄 소개", null,
                        StaffRecruitmentQuestionType.SHORT_TEXT, false, 50, 2)), false);
        assignIds(recruitment.getQuestions());
        Integer motiveId = recruitment.getQuestions().get(0).getId();

        assertThatThrownBy(() -> recruitment.mergeQuestions(List.of(
                new StaffRecruitmentQuestionSpec(motiveId, "지원 동기", null,
                        StaffRecruitmentQuestionType.LONG_TEXT, true, 1000, 1)), true))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_STRUCTURE_LOCKED);
    }

    /** 저장 전에는 식별자가 없으므로, 병합 규칙을 검증하려면 저장된 상태를 흉내 내야 한다. */
    private StaffRecruitment withDepartments(String... names) {
        StaffRecruitment recruitment = draft();
        List<StaffRecruitmentDepartmentSpec> specs = IntStream.range(0, names.length)
                .mapToObj(index -> new StaffRecruitmentDepartmentSpec(null, names[index], null, index + 1, true))
                .toList();
        recruitment.mergeDepartments(specs, false);
        assignIds(recruitment.getDepartments());
        return recruitment;
    }

    private static void assignIds(List<?> entities) {
        for (int i = 0; i < entities.size(); i++) {
            ReflectionTestUtils.setField(entities.get(i), "id", i + 1);
        }
    }
}
