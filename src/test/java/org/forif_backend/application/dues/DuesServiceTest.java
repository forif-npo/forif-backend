package org.forif_backend.application.dues;

import org.forif_backend.application.dues.dto.DuesPageResult;
import org.forif_backend.application.dues.dto.UpdateDuesMemberCommand;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.dues.MemberSemesterCheck;
import org.forif_backend.domain.dues.MemberSemesterCheckRepository;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyRepository;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DuesServiceTest {

    private static final SemesterInfo CURRENT_SEMESTER = SemesterInfo.of(2026, 2);

    @Mock
    private SemesterService semesterService;
    @Mock
    private StudyUserRepository studyUserRepository;
    @Mock
    private StudyRepository studyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserApplyRepository userApplyRepository;
    @Mock
    private MemberSemesterCheckRepository memberSemesterCheckRepository;

    @InjectMocks
    private DuesService duesService;

    private User duesUnpaidUser;
    private User completedUser;

    @BeforeEach
    void setUp() {
        duesUnpaidUser = User.createUser(1L, "가나다", "first@hanyang.ac.kr", "01011112222", "컴퓨터학부");
        completedUser = User.createUser(2L, "나다라마바사", "second@hanyang.ac.kr", "01033334444", "전자공학부");

        when(semesterService.getActive()).thenReturn(CURRENT_SEMESTER);
    }

    @Test
    @DisplayName("상태가 없는 현재 학기 합격자는 미납·미제출로 조회하고 확인 필요 순서로 정렬한다")
    void getsCurrentSemesterDuesWithDefaultUncheckedStatus() {
        MemberSemesterCheck completedCheck = MemberSemesterCheck.create(completedUser, 2026, 2);
        completedCheck.update(true, true);

        when(userApplyRepository.findAcceptedApplicantsByYearSemester(2026, 2, null))
                .thenReturn(List.of(completedUser, duesUnpaidUser));
        when(memberSemesterCheckRepository.findAllByYearSemesterAndUserIds(2026, 2, List.of(2L, 1L)))
                .thenReturn(List.of(completedCheck));

        DuesPageResult result = duesService.getCurrentSemesterDues(0, 20, null, List.of());

        assertThat(result.content()).extracting(member -> member.userId())
                .containsExactly(1L, 2L);
        assertThat(result.content().get(0).duesPaid()).isFalse();
        assertThat(result.content().get(0).googleFormSubmitted()).isFalse();
        assertThat(result.summary().totalCount()).isEqualTo(2);
        assertThat(result.summary().completedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("체크 상태를 처음 수정하면 현재 학기 상태 행을 생성한다")
    void createsMemberCheckWhenUpdatingForTheFirstTime() {
        when(userApplyRepository.existsAcceptedByApplierIdAndYearSemester(1L, 2026, 2)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(duesUnpaidUser));
        when(memberSemesterCheckRepository.findByUserIdAndYearSemester(1L, 2026, 2))
                .thenReturn(Optional.empty());
        when(memberSemesterCheckRepository.save(any(MemberSemesterCheck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        duesService.updateCurrentSemesterDuesBatch(List.of(
                new UpdateDuesMemberCommand(1L, true, false)
        ));

        ArgumentCaptor<MemberSemesterCheck> captor = ArgumentCaptor.forClass(MemberSemesterCheck.class);
        verify(memberSemesterCheckRepository).save(captor.capture());
        assertThat(captor.getValue().isDuesPaid()).isTrue();
        assertThat(captor.getValue().isGoogleFormSubmitted()).isFalse();
    }

    @Test
    @DisplayName("회비 관리 목록은 현재 학기 합격자만 조회한다")
    void includesOnlyAcceptedApplicants() {
        User acceptedApplicant = User.createUser(3L, "다라마바사", "applicant@hanyang.ac.kr", "01055556666", "소프트웨어학부");
        when(userApplyRepository.findAcceptedApplicantsByYearSemester(2026, 2, null))
                .thenReturn(List.of(duesUnpaidUser, acceptedApplicant));
        when(memberSemesterCheckRepository.findAllByYearSemesterAndUserIds(2026, 2, List.of(1L, 3L)))
                .thenReturn(List.of());

        DuesPageResult result = duesService.getCurrentSemesterDues(
                0, 20, null, List.of(new SortCriteria("userName", SortDirection.ASC)));

        assertThat(result.content()).extracting(member -> member.userId())
                .containsExactly(1L, 3L);
        assertThat(result.summary().totalCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("회비 납부 여부로 현재 학기 합격자를 필터링할 수 있다")
    void filtersCurrentSemesterDuesByPaidStatus() {
        MemberSemesterCheck completedCheck = MemberSemesterCheck.create(completedUser, 2026, 2);
        completedCheck.update(true, true);

        when(userApplyRepository.findAcceptedApplicantsByYearSemester(2026, 2, null))
                .thenReturn(List.of(completedUser, duesUnpaidUser));
        when(memberSemesterCheckRepository.findAllByYearSemesterAndUserIds(2026, 2, List.of(2L, 1L)))
                .thenReturn(List.of(completedCheck));

        DuesPageResult result = duesService.getCurrentSemesterDues(
                0, 20, null, false, null, List.of());

        assertThat(result.content()).extracting(member -> member.userId())
                .containsExactly(1L);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.summary().totalCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("구글폼 제출 여부로 현재 학기 합격자를 필터링할 수 있다")
    void filtersCurrentSemesterDuesByGoogleFormSubmittedStatus() {
        MemberSemesterCheck completedCheck = MemberSemesterCheck.create(completedUser, 2026, 2);
        completedCheck.update(true, true);

        when(userApplyRepository.findAcceptedApplicantsByYearSemester(2026, 2, null))
                .thenReturn(List.of(completedUser, duesUnpaidUser));
        when(memberSemesterCheckRepository.findAllByYearSemesterAndUserIds(2026, 2, List.of(2L, 1L)))
                .thenReturn(List.of(completedCheck));

        DuesPageResult result = duesService.getCurrentSemesterDues(
                0, 20, null, null, false, List.of());

        assertThat(result.content()).extracting(member -> member.userId())
                .containsExactly(1L);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("회비 미납과 구글폼 미제출 필터를 함께 적용하면 두 조건 모두 미확인인 합격자만 조회한다")
    void filtersCurrentSemesterDuesByBothUncheckedStatuses() {
        User duesPaidOnlyUser = User.createUser(3L, "다라마바사", "third@hanyang.ac.kr", "01055556666", "소프트웨어학부");
        User googleFormSubmittedOnlyUser = User.createUser(4L, "라마바사아", "fourth@hanyang.ac.kr", "01077778888", "기계공학부");
        MemberSemesterCheck completedCheck = MemberSemesterCheck.create(completedUser, 2026, 2);
        MemberSemesterCheck duesPaidOnlyCheck = MemberSemesterCheck.create(duesPaidOnlyUser, 2026, 2);
        MemberSemesterCheck googleFormSubmittedOnlyCheck = MemberSemesterCheck.create(googleFormSubmittedOnlyUser, 2026, 2);
        completedCheck.update(true, true);
        duesPaidOnlyCheck.update(true, false);
        googleFormSubmittedOnlyCheck.update(false, true);

        when(userApplyRepository.findAcceptedApplicantsByYearSemester(2026, 2, null))
                .thenReturn(List.of(
                        completedUser,
                        duesPaidOnlyUser,
                        googleFormSubmittedOnlyUser,
                        duesUnpaidUser
                ));
        when(memberSemesterCheckRepository.findAllByYearSemesterAndUserIds(
                2026, 2, List.of(2L, 3L, 4L, 1L)))
                .thenReturn(List.of(completedCheck, duesPaidOnlyCheck, googleFormSubmittedOnlyCheck));

        DuesPageResult result = duesService.getCurrentSemesterDues(
                0, 20, null, false, false, List.of());

        assertThat(result.content()).extracting(member -> member.userId())
                .containsExactly(1L);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.summary())
                .extracting(summary -> summary.totalCount(),
                        summary -> summary.duesPaidCount(),
                        summary -> summary.googleFormSubmittedCount(),
                        summary -> summary.completedCount())
                .containsExactly(4, 2, 2, 1);
    }

    @Test
    @DisplayName("매우 큰 페이지 번호는 빈 회비 목록으로 처리한다")
    void returnsEmptyDuesPageForAnExcessivelyLargePageNumber() {
        when(userApplyRepository.findAcceptedApplicantsByYearSemester(2026, 2, null))
                .thenReturn(List.of(duesUnpaidUser));
        when(memberSemesterCheckRepository.findAllByYearSemesterAndUserIds(2026, 2, List.of(1L)))
                .thenReturn(List.of());

        DuesPageResult result = duesService.getCurrentSemesterDues(
                Integer.MAX_VALUE, 100, null, List.of());

        assertThat(result.content()).isEmpty();
    }

    @Test
    @DisplayName("현재 학기 합격자는 수강생 등록 전에도 회비 상태를 저장할 수 있다")
    void updatesDuesForAcceptedApplicantWhoIsNotStudyMember() {
        when(userApplyRepository.existsAcceptedByApplierIdAndYearSemester(1L, 2026, 2)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(duesUnpaidUser));
        when(memberSemesterCheckRepository.findByUserIdAndYearSemester(1L, 2026, 2))
                .thenReturn(Optional.empty());
        when(memberSemesterCheckRepository.save(any(MemberSemesterCheck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        duesService.updateCurrentSemesterDuesBatch(List.of(
                new UpdateDuesMemberCommand(1L, true, false)
        ));

        verify(memberSemesterCheckRepository).save(any(MemberSemesterCheck.class));
    }

    @Test
    @DisplayName("합격하지 않은 신청자는 회비 상태를 저장할 수 없다")
    void rejectsDuesUpdateForApplicantWithoutAcceptedStudy() {
        when(userApplyRepository.existsAcceptedByApplierIdAndYearSemester(1L, 2026, 2)).thenReturn(false);

        assertThatThrownBy(() -> duesService.updateCurrentSemesterDuesBatch(List.of(
                new UpdateDuesMemberCommand(1L, true, true)
        )))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.CURRENT_SEMESTER_MEMBER_NOT_FOUND));

        verify(memberSemesterCheckRepository, never()).save(any(MemberSemesterCheck.class));
    }

    @Test
    @DisplayName("합격자는 회비와 구글폼이 모두 확인될 때에만 수강생으로 등록한다")
    void registersAcceptedApplicantOnlyWhenBothChecksAreComplete() {
        Study acceptedStudy = mock(Study.class);
        UserApply acceptedApplication = mock(UserApply.class);
        MemberSemesterCheck completedCheck = MemberSemesterCheck.create(duesUnpaidUser, 2026, 2);
        completedCheck.update(true, true);

        when(userApplyRepository.existsAcceptedByApplierIdAndYearSemester(1L, 2026, 2)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(duesUnpaidUser));
        when(memberSemesterCheckRepository.findByUserIdAndYearSemester(1L, 2026, 2))
                .thenReturn(Optional.of(completedCheck));
        when(userApplyRepository.findByApplierIdAndYearSemester(1L, 2026, 2))
                .thenReturn(Optional.of(acceptedApplication));
        when(acceptedApplication.getPrimaryStatus()).thenReturn(UserApplyStatus.ACCEPT);
        when(acceptedApplication.getPrimaryStudy()).thenReturn(10);
        when(studyRepository.findStudyById(10)).thenReturn(Optional.of(acceptedStudy));
        when(acceptedStudy.getId()).thenReturn(10);
        when(studyUserRepository.findByUserIdAndStudyId(1L, 10)).thenReturn(Optional.empty());

        duesService.updateCurrentSemesterDuesBatch(List.of(
                new UpdateDuesMemberCommand(1L, true, true)
        ));

        verify(studyUserRepository).save(any(StudyUser.class));
    }

    @Test
    @DisplayName("확인 완료 후 회비 또는 구글폼 상태가 취소되면 수강생 등록을 해제한다")
    void removesStudyMemberWhenARequiredCheckIsCancelled() {
        Study acceptedStudy = mock(Study.class);
        UserApply acceptedApplication = mock(UserApply.class);
        MemberSemesterCheck completedCheck = MemberSemesterCheck.create(duesUnpaidUser, 2026, 2);
        completedCheck.update(true, true);

        when(userApplyRepository.existsAcceptedByApplierIdAndYearSemester(1L, 2026, 2)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(duesUnpaidUser));
        when(memberSemesterCheckRepository.findByUserIdAndYearSemester(1L, 2026, 2))
                .thenReturn(Optional.of(completedCheck));
        when(userApplyRepository.findByApplierIdAndYearSemester(1L, 2026, 2))
                .thenReturn(Optional.of(acceptedApplication));
        when(acceptedApplication.getPrimaryStatus()).thenReturn(UserApplyStatus.ACCEPT);
        when(acceptedApplication.getPrimaryStudy()).thenReturn(10);
        when(studyRepository.findStudyById(10)).thenReturn(Optional.of(acceptedStudy));
        when(acceptedStudy.getId()).thenReturn(10);

        duesService.updateCurrentSemesterDuesBatch(List.of(
                new UpdateDuesMemberCommand(1L, false, null)
        ));

        verify(studyUserRepository).deleteByUserIdAndStudyId(1L, 10);
    }
}
