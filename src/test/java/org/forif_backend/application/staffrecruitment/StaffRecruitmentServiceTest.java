package org.forif_backend.application.staffrecruitment;

import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.team.ClubDepartmentService;
import org.forif_backend.application.staffrecruitment.dto.CreateStaffRecruitmentCommand;
import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentDepartmentCommand;
import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentDetail;
import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentQuestionCommand;
import org.forif_backend.application.staffrecruitment.dto.UpdateStaffRecruitmentCommand;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.RecruitmentApplicationStatus;
import org.forif_backend.domain.staffrecruitment.StaffApplicationRepository;
import org.forif_backend.domain.staffrecruitment.StaffInterviewQuestion;
import org.forif_backend.domain.staffrecruitment.StaffInterviewQuestionRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentDepartmentSpec;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentStatus;
import org.forif_backend.domain.team.ClubDepartment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StaffRecruitmentServiceTest {

    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2026, 12, 1, 0, 0);
    private static final LocalDateTime ENDS_AT = LocalDateTime.of(2026, 12, 15, 18, 0);

    @Mock
    private StaffRecruitmentRepository staffRecruitmentRepository;

    @Mock
    private StaffApplicationRepository staffApplicationRepository;

    @Mock
    private StaffInterviewQuestionRepository staffInterviewQuestionRepository;

    @Mock
    private SemesterService semesterService;

    @Mock
    private ClubDepartmentService clubDepartmentService;

    @InjectMocks
    private StaffRecruitmentService staffRecruitmentService;

    private StaffRecruitment recruitment(Integer id, boolean publish) {
        return recruitment(id, publish, STARTS_AT, ENDS_AT);
    }

    /** 접수 상태는 현재 시각으로 계산하므로 상태를 검증할 때는 상대 시각을 쓴다. */
    private StaffRecruitment recruitment(Integer id, boolean publish, LocalDateTime startsAt, LocalDateTime endsAt) {
        StaffRecruitment recruitment =
                StaffRecruitment.create(2027, 1, "2027-1 운영진 모집", null, null, startsAt, endsAt);
        if (publish) {
            recruitment.publish();
        }
        ReflectionTestUtils.setField(recruitment, "id", id);
        return recruitment;
    }

    @Test
    void 학기를_지정하지_않으면_현재_활동_학기를_값으로_복사한다() {
        ClubDepartment planning = ClubDepartment.create("기획");
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2027, 1));
        when(staffRecruitmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clubDepartmentService.getOrCreate("기획")).thenReturn(planning);

        StaffRecruitmentDetail detail = staffRecruitmentService.create(new CreateStaffRecruitmentCommand(
                null, null, "2027-1 운영진 모집", "요약", "안내", STARTS_AT, ENDS_AT,
                List.of(new StaffRecruitmentDepartmentCommand(null, "기획팀", null, 1, true)),
                List.of(new StaffRecruitmentQuestionCommand(null, "지원 동기", null,
                        StaffRecruitmentQuestionType.LONG_TEXT, true, 1000, 1))));

        assertThat(detail.actYear()).isEqualTo(2027);
        assertThat(detail.actSemester()).isEqualTo(1);
        assertThat(detail.semesterLabel()).isEqualTo("27-1");
        assertThat(detail.publicationStatus()).isEqualTo(StaffRecruitmentStatus.DRAFT);
        assertThat(detail.departments()).hasSize(1);
        assertThat(detail.departments().get(0).name()).isEqualTo("기획");
        ArgumentCaptor<StaffRecruitment> saved = ArgumentCaptor.forClass(StaffRecruitment.class);
        verify(staffRecruitmentRepository).save(saved.capture());
        assertThat(saved.getValue().getDepartments().get(0).getClubDepartment()).isSameAs(planning);
        assertThat(detail.questions()).hasSize(1);
    }

    @Test
    void 파일_문항의_내부_답변_길이는_클라이언트_값과_무관하게_설정한다() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2027, 1));
        when(staffRecruitmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clubDepartmentService.getOrCreate("기획")).thenReturn(ClubDepartment.create("기획"));

        StaffRecruitmentDetail detail = staffRecruitmentService.create(new CreateStaffRecruitmentCommand(
                null, null, "2027-1 운영진 모집", null, "안내", STARTS_AT, ENDS_AT,
                List.of(new StaffRecruitmentDepartmentCommand(null, "기획팀", null, 1, true)),
                List.of(new StaffRecruitmentQuestionCommand(null, "포트폴리오", null,
                        StaffRecruitmentQuestionType.FILE, true, 1, 1))));

        assertThat(detail.questions().get(0).maxLength()).isEqualTo(5000);
    }

    @Test
    void 게시_중인_다른_회차가_있으면_게시를_거절한다() {
        StaffRecruitment target = recruitment(2, false);
        when(staffRecruitmentRepository.findByIdForUpdate(2)).thenReturn(Optional.of(target));
        when(staffRecruitmentRepository.findPublished()).thenReturn(Optional.of(recruitment(1, true)));

        assertThatThrownBy(() -> staffRecruitmentService.publish(2, null))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_ACTIVE_CONFLICT);
        assertThat(target.getStatus()).isEqualTo(StaffRecruitmentStatus.DRAFT);
    }

    @Test
    void 화면이_가진_version이_낡았으면_수정을_거절한다() {
        StaffRecruitment target = recruitment(3, false);
        ReflectionTestUtils.setField(target, "version", 2L);
        when(staffRecruitmentRepository.findByIdForUpdate(3)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> staffRecruitmentService.update(3, new UpdateStaffRecruitmentCommand(
                1L, "바뀐 제목", null, null, STARTS_AT, ENDS_AT, null, null)))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_INTEGRITY_VIOLATION);
        assertThat(target.getTitle()).isEqualTo("2027-1 운영진 모집");
    }

    @Test
    void 게시된_회차는_수정_요청을_거절한다() {
        StaffRecruitment target = recruitment(3, true);
        when(staffRecruitmentRepository.findByIdForUpdate(3)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> staffRecruitmentService.update(3, new UpdateStaffRecruitmentCommand(
                null, "바뀐 제목", null, null, STARTS_AT, ENDS_AT, null, null)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_INVALID_STATUS);
        assertThat(target.getTitle()).isEqualTo("2027-1 운영진 모집");
    }

    @Test
    void 면접_질문이_있는_부서는_초안에서도_삭제할_수_없다() {
        StaffRecruitment target = recruitmentWithDepartments(7);
        when(staffRecruitmentRepository.findByIdForUpdate(7)).thenReturn(Optional.of(target));
        when(staffInterviewQuestionRepository.findAllByRecruitmentId(7)).thenReturn(List.of(
                StaffInterviewQuestion.create(target, target.getDepartments().get(0), "면접 질문", 1)));

        assertThatThrownBy(() -> staffRecruitmentService.update(7, updateKeepingDepartment(12)))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        ErrorCode.STAFF_RECRUITMENT_DEPARTMENT_HAS_INTERVIEW_QUESTIONS);
        assertThat(target.getDepartments()).hasSize(2);
    }

    @Test
    void 삭제하지_않는_부서의_면접_질문은_다른_부서_삭제를_막지_않는다() {
        StaffRecruitment target = recruitmentWithDepartments(8);
        when(staffRecruitmentRepository.findByIdForUpdate(8)).thenReturn(Optional.of(target));
        when(staffInterviewQuestionRepository.findAllByRecruitmentId(8)).thenReturn(List.of(
                StaffInterviewQuestion.create(target, null, "공통 질문", 1),
                StaffInterviewQuestion.create(target, target.getDepartments().get(0), "기획팀 질문", 1)));
        when(clubDepartmentService.getOrCreate("기획")).thenReturn(ClubDepartment.create("기획"));

        StaffRecruitmentDetail detail = staffRecruitmentService.update(8, updateKeepingDepartment(11));

        assertThat(detail.departments()).extracting(StaffRecruitmentDetail.DepartmentInfo::name)
                .containsExactly("기획");
    }

    private StaffRecruitment recruitmentWithDepartments(int recruitmentId) {
        StaffRecruitment target = recruitment(recruitmentId, false);
        target.mergeDepartments(List.of(
                new StaffRecruitmentDepartmentSpec(null, "기획팀", null, 1, true),
                new StaffRecruitmentDepartmentSpec(null, "SW팀", null, 2, true)), false);
        ReflectionTestUtils.setField(target.getDepartments().get(0), "id", 11);
        ReflectionTestUtils.setField(target.getDepartments().get(1), "id", 12);
        return target;
    }

    private UpdateStaffRecruitmentCommand updateKeepingDepartment(int departmentId) {
        String name = departmentId == 11 ? "기획팀" : "SW팀";
        return new UpdateStaffRecruitmentCommand(null, "2027-1 운영진 모집", null, null,
                STARTS_AT, ENDS_AT,
                List.of(new StaffRecruitmentDepartmentCommand(departmentId, name, null, 1, true)), null);
    }

    @Test
    void 초안은_공개_상세에서_찾을_수_없다() {
        when(staffRecruitmentRepository.findDetailById(4)).thenReturn(Optional.of(recruitment(4, false)));

        assertThatThrownBy(() -> staffRecruitmentService.getPublicDetail(4))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_RECRUITMENT_NOT_FOUND);
    }

    @Test
    void 시작_전_회차는_UPCOMING_사유와_함께_공개된다() {
        LocalDateTime now = LocalDateTime.now();
        when(staffRecruitmentRepository.findPublished())
                .thenReturn(Optional.of(recruitment(6, true, now.plusDays(1), now.plusDays(2))));

        StaffRecruitmentDetail detail = staffRecruitmentService.getPublicCurrent().orElseThrow();

        assertThat(detail.applicationStatus()).isEqualTo(RecruitmentApplicationStatus.UPCOMING);
        assertThat(detail.applicationOpen()).isFalse();
        assertThat(detail.applicationUnavailableReason()).isEqualTo("UPCOMING");
    }

    @Test
    void 게시된_회차가_없으면_현재_모집은_비어_있다() {
        when(staffRecruitmentRepository.findPublished()).thenReturn(Optional.empty());

        assertThat(staffRecruitmentService.getPublicCurrent()).isEmpty();
    }

    @Test
    void 게시된_회차는_접수_상태와_함께_공개된다() {
        LocalDateTime now = LocalDateTime.now();
        when(staffRecruitmentRepository.findPublished())
                .thenReturn(Optional.of(recruitment(5, true, now.minusDays(1), now.plusDays(1))));

        Optional<StaffRecruitmentDetail> current = staffRecruitmentService.getPublicCurrent();

        assertThat(current).isPresent();
        assertThat(current.get().publicationStatus()).isEqualTo(StaffRecruitmentStatus.PUBLISHED);
        assertThat(current.get().applicationStatus()).isEqualTo(RecruitmentApplicationStatus.OPEN);
        assertThat(current.get().applicationOpen()).isTrue();
        assertThat(current.get().applicationUnavailableReason()).isNull();
    }
}
