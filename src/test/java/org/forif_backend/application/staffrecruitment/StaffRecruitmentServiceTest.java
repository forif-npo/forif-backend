package org.forif_backend.application.staffrecruitment;

import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.staffrecruitment.dto.CreateStaffRecruitmentCommand;
import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentDepartmentCommand;
import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentDetail;
import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentQuestionCommand;
import org.forif_backend.application.staffrecruitment.dto.UpdateStaffRecruitmentCommand;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.RecruitmentApplicationStatus;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentStatus;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffRecruitmentServiceTest {

    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2026, 12, 1, 0, 0);
    private static final LocalDateTime ENDS_AT = LocalDateTime.of(2026, 12, 15, 18, 0);

    @Mock
    private StaffRecruitmentRepository staffRecruitmentRepository;

    @Mock
    private SemesterService semesterService;

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
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2027, 1));
        when(staffRecruitmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StaffRecruitmentDetail detail = staffRecruitmentService.create(new CreateStaffRecruitmentCommand(
                null, null, "2027-1 운영진 모집", "요약", "안내", STARTS_AT, ENDS_AT,
                List.of(new StaffRecruitmentDepartmentCommand("기획팀", null, 1, true)),
                List.of(new StaffRecruitmentQuestionCommand("지원 동기", null,
                        StaffRecruitmentQuestionType.LONG_TEXT, true, 1000, 1))));

        assertThat(detail.actYear()).isEqualTo(2027);
        assertThat(detail.actSemester()).isEqualTo(1);
        assertThat(detail.semesterLabel()).isEqualTo("27-1");
        assertThat(detail.publicationStatus()).isEqualTo(StaffRecruitmentStatus.DRAFT);
        assertThat(detail.departments()).hasSize(1);
        assertThat(detail.questions()).hasSize(1);
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
