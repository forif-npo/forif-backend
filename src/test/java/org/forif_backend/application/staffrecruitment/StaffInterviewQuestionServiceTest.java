package org.forif_backend.application.staffrecruitment;

import org.forif_backend.application.staffrecruitment.dto.StaffInterviewQuestionDetail;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffInterviewQuestionServiceTest {

    private static final Integer RECRUITMENT_ID = 12;
    private static final Integer SW_TEAM = 31;

    @Mock
    private StaffRecruitmentRepository staffRecruitmentRepository;

    @Mock
    private StaffInterviewQuestionRepository staffInterviewQuestionRepository;

    @InjectMocks
    private StaffInterviewQuestionService staffInterviewQuestionService;

    private StaffRecruitment recruitment() {
        LocalDateTime now = LocalDateTime.now();
        StaffRecruitment recruitment = StaffRecruitment.create(
                2026, 2, "2026-2 운영진 모집", null, null, now.minusDays(1), now.plusDays(1));
        recruitment.mergeDepartments(List.of(
                new StaffRecruitmentDepartmentSpec(null, "SW팀", null, 1, true)), false);
        ReflectionTestUtils.setField(recruitment, "id", RECRUITMENT_ID);
        ReflectionTestUtils.setField(recruitment.getDepartments().get(0), "id", SW_TEAM);
        return recruitment;
    }

    @Test
    void 부서를_비우면_공통_질문이_된다() {
        StaffRecruitment recruitment = recruitment();
        when(staffRecruitmentRepository.findDetailById(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment));
        when(staffInterviewQuestionRepository.findAllByRecruitmentId(RECRUITMENT_ID)).thenReturn(List.of());
        when(staffInterviewQuestionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StaffInterviewQuestionDetail detail =
                staffInterviewQuestionService.create(RECRUITMENT_ID, null, "  지원 동기를 말해 주세요.  ");

        assertThat(detail.departmentId()).isNull();
        assertThat(detail.content()).isEqualTo("지원 동기를 말해 주세요.");
        assertThat(detail.displayOrder()).isEqualTo(1);
    }

    @Test
    void 같은_부서_질문_뒤에_순서를_이어_붙인다() {
        StaffRecruitment recruitment = recruitment();
        StaffRecruitmentDepartment sw = recruitment.getDepartments().get(0);
        when(staffRecruitmentRepository.findDetailById(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment));
        when(staffInterviewQuestionRepository.findAllByRecruitmentId(RECRUITMENT_ID)).thenReturn(List.of(
                StaffInterviewQuestion.create(recruitment, null, "공통 1", 1),
                StaffInterviewQuestion.create(recruitment, null, "공통 2", 2),
                StaffInterviewQuestion.create(recruitment, sw, "SW 1", 1)));
        when(staffInterviewQuestionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StaffInterviewQuestionDetail detail =
                staffInterviewQuestionService.create(RECRUITMENT_ID, SW_TEAM, "써 본 프레임워크는?");

        assertThat(detail.departmentId()).isEqualTo(SW_TEAM);
        assertThat(detail.departmentName()).isEqualTo("SW팀");
        assertThat(detail.displayOrder()).isEqualTo(2);
    }

    @Test
    void 이_회차에_없는_부서에는_질문을_달_수_없다() {
        when(staffRecruitmentRepository.findDetailById(RECRUITMENT_ID)).thenReturn(Optional.of(recruitment()));

        assertThatThrownBy(() -> staffInterviewQuestionService.create(RECRUITMENT_ID, 999, "질문"))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        verify(staffInterviewQuestionRepository, never()).save(any());
    }

    @Test
    void 빈_질문은_만들_수_없다() {
        StaffRecruitment recruitment = recruitment();

        assertThatThrownBy(() -> StaffInterviewQuestion.create(recruitment, null, "   ", 1))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 다른_회차의_질문은_고칠_수_없다() {
        StaffRecruitment other = recruitment();
        ReflectionTestUtils.setField(other, "id", 99);
        when(staffInterviewQuestionRepository.findById(7))
                .thenReturn(Optional.of(StaffInterviewQuestion.create(other, null, "질문", 1)));

        assertThatThrownBy(() -> staffInterviewQuestionService.updateContent(RECRUITMENT_ID, 7, "고친 질문"))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_INTERVIEW_QUESTION_NOT_FOUND);
    }
}
