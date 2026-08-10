package org.forif_backend.application.study;

import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyStatus;
import org.forif_backend.domain.study.StudyTag;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.study.dto.UpdateStudyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyServiceApplicationUpdateTest {

    @Mock private SemesterService semesterService;
    @Mock private SemesterPhaseGuard semesterPhaseGuard;
    @Mock private StudyRecruitStatusPolicy recruitStatusPolicy;
    @Mock private StudyMentorAccess studyMentorAccess;
    @Mock private StudyRepository studyRepository;
    @Mock private StudyUserRepository studyUserRepository;
    @Mock private org.forif_backend.domain.study.StudyAttendanceRepository studyAttendanceRepository;
    @Mock private UserRepository userRepository;
    @Mock private org.forif_backend.domain.user.UserApplyRepository userApplyRepository;
    @Mock private FilePort filePort;
    @Mock private StaffAccountService staffAccountService;
    @Mock private StaffAccountRepository staffAccountRepository;

    private StudyService studyService;

    @BeforeEach
    void setUp() {
        studyService = new StudyService(
                semesterService,
                semesterPhaseGuard,
                recruitStatusPolicy,
                studyMentorAccess,
                studyRepository,
                studyUserRepository,
                studyAttendanceRepository,
                userRepository,
                userApplyRepository,
                filePort,
                staffAccountService,
                staffAccountRepository
        );
    }

    @Test
    void rejectsUpdatingAnApplicationOutsideTheActiveSemester() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        doThrow(new ForifException(ErrorCode.STUDY_NOT_IN_ACTIVE_SEMESTER))
                .when(studyMentorAccess)
                .requireMentorOfActiveSemester(study, 10L);

        assertThatThrownBy(() -> studyService.updateStudyApplication(
                1, 10L, new UpdateStudyRequest(), null, null))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.STUDY_NOT_IN_ACTIVE_SEMESTER));

        verify(semesterPhaseGuard, never()).requireOpen(org.forif_backend.domain.semester.SemesterPhase.MENTOR_RECRUIT);
        verify(study, never()).reApply();
    }

    @Test
    void marksPastSemesterApplicationsAsNotModifiable() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyApplicationsByMentorId(10L)).thenReturn(List.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.REJECTED);
        when(study.getActYear()).thenReturn(2024);
        when(study.getTags()).thenReturn(List.of());
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 1));

        boolean canModify = studyService.getMyStudyApplications(10L).get(0).isCanModify();

        assertThat(canModify).isFalse();
        verify(semesterPhaseGuard, never()).isOpen(
                org.forif_backend.domain.semester.SemesterPhase.MENTOR_RECRUIT, 2024, 2);
    }

    @Test
    void keepsExistingPlansWhenTheUpdateRequestOmitsStudyPlanList() {
        Study study = mock(Study.class);
        StudyTag tag = mock(StudyTag.class);
        UpdateStudyRequest request = new UpdateStudyRequest();
        request.setStudyTagNames(List.of("ai"));

        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.PENDING);
        when(studyRepository.findAllStudyTagByName(List.of("ai"))).thenReturn(List.of(tag));

        studyService.updateStudyApplication(1, 10L, request, null, null);

        verify(studyRepository, never()).deleteStudyPlansByStudyId(1);
        verify(studyRepository).findAllStudyTagByName(List.of("ai"));
    }

    @Test
    void updatesOnlyExplanationWithoutReplacingPlans() {
        Study study = mock(Study.class);
        UpdateStudyRequest request = new UpdateStudyRequest();
        request.setExplanation("수정된 스터디 소개입니다. 충분히 긴 설명을 입력했습니다.");

        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.PENDING);

        studyService.updateStudyApplication(1, 10L, request, null, null);

        verify(study).setExplanation(request.getExplanation());
        verify(studyRepository, never()).deleteStudyPlansByStudyId(1);
        verify(studyRepository, never()).saveAllStudyPlan(org.mockito.ArgumentMatchers.anyList());
    }
}
