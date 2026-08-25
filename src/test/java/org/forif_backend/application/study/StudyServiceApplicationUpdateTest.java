package org.forif_backend.application.study;

import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.file.dto.FileInfo;
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
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.study.dto.CreateStudyApplyRequest;
import org.forif_backend.web.study.dto.UpdateStudyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.forif_backend.domain.study.ReferenceType;
import org.forif_backend.domain.study.StudyReference;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

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
    @Mock private org.forif_backend.domain.study.MentorConfirmationRepository mentorConfirmationRepository;

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
                staffAccountRepository,
                mentorConfirmationRepository
        );
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rejectsUpdatingAnApplicationOutsideTheActiveSemester() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        doThrow(new ForifException(ErrorCode.STUDY_NOT_IN_ACTIVE_SEMESTER))
                .when(studyMentorAccess)
                .requireMentorOfActiveSemester(study, 10L);

        assertThatThrownBy(() -> studyService.updateStudyApplication(
                1, 10L, new UpdateStudyRequest().toCommand(), null, null))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.STUDY_NOT_IN_ACTIVE_SEMESTER));

        verify(semesterPhaseGuard, never()).requireBeforeStart(org.forif_backend.domain.semester.SemesterPhase.MENTEE_RECRUIT);
        verify(study, never()).reApply();
    }

    @Test
    void blocksApprovingAStudyOutsideTheActiveSemester() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyById(1)).thenReturn(Optional.of(study));
        when(study.getActYear()).thenReturn(2025);
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 1));

        assertThatThrownBy(() -> studyService.approveStudy(1))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.STUDY_NOT_IN_ACTIVE_SEMESTER));

        verify(study, never()).approve();
    }

    @Test
    void marksPastSemesterApplicationsAsNotModifiable() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyApplicationsByMentorId(10L, 2026, 1)).thenReturn(List.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.REJECTED);
        when(study.getActYear()).thenReturn(2024);
        when(study.getTags()).thenReturn(List.of());
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 1));

        boolean canModify = studyService.getMyStudyApplications(10L).get(0).isCanModify();

        assertThat(canModify).isFalse();
        verify(semesterPhaseGuard, never()).isBeforeStart(
                org.forif_backend.domain.semester.SemesterPhase.MENTEE_RECRUIT, 2024, 2);
    }

    @Test
    void allowsModificationButNotCancellationBetweenRecruitmentPeriods() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyApplicationsByMentorId(10L, 2026, 1)).thenReturn(List.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.PENDING);
        when(study.getActYear()).thenReturn(2026);
        when(study.getActSemester()).thenReturn(1);
        when(study.getTags()).thenReturn(List.of());
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 1));
        when(semesterPhaseGuard.isBeforeStart(
                org.forif_backend.domain.semester.SemesterPhase.MENTEE_RECRUIT, 2026, 1))
                .thenReturn(true);
        when(semesterPhaseGuard.isOpen(
                org.forif_backend.domain.semester.SemesterPhase.MENTOR_RECRUIT, 2026, 1))
                .thenReturn(false);

        var application = studyService.getMyStudyApplications(10L).get(0);

        assertThat(application.isCanModify()).isTrue();
        assertThat(application.isCanCancel()).isFalse();
    }

    @Test
    void doesNotExposeRejectedApplicationAsModifiableAfterMentorReviewEnds() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyApplicationsByMentorId(10L, 2026, 1)).thenReturn(List.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.REJECTED);
        when(study.getActYear()).thenReturn(2026);
        when(study.getActSemester()).thenReturn(1);
        when(study.getTags()).thenReturn(List.of());
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 1));
        when(semesterPhaseGuard.isBeforeStart(
                org.forif_backend.domain.semester.SemesterPhase.MENTEE_RECRUIT, 2026, 1))
                .thenReturn(true);
        when(semesterPhaseGuard.isOpen(
                org.forif_backend.domain.semester.SemesterPhase.MENTOR_REVIEW, 2026, 1))
                .thenReturn(false);

        boolean canModify = studyService.getMyStudyApplications(10L).get(0).isCanModify();

        assertThat(canModify).isFalse();
    }

    @Test
    void blocksReapplyingRejectedApplicationAfterMentorReviewEnds() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.REJECTED);
        doThrow(new ForifException(ErrorCode.SEMESTER_PHASE_CLOSED))
                .when(semesterPhaseGuard)
                .requireOpen(org.forif_backend.domain.semester.SemesterPhase.MENTOR_REVIEW);

        assertThatThrownBy(() -> studyService.updateStudyApplication(
                1, 10L, new UpdateStudyRequest().toCommand(), null, null))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.SEMESTER_PHASE_CLOSED));

        verify(study, never()).reApply();
    }

    @Test
    void allowsApprovedApplicationModificationBeforeMenteeRecruitmentStarts() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);

        studyService.updateStudyApplication(1, 10L, new UpdateStudyRequest().toCommand(), null, null);

        verify(semesterPhaseGuard).requireBeforeStart(
                org.forif_backend.domain.semester.SemesterPhase.MENTEE_RECRUIT);
        verify(study, never()).reApply();
    }

    @Test
    void allowsUpdatingApprovedApplicationOperationalFields() {
        Study study = mock(Study.class);
        StudyTag tag = mock(StudyTag.class);
        UpdateStudyRequest request = new UpdateStudyRequest();
        request.setExplanation("수정된 상세 소개입니다.");
        request.setDifficulty(2);
        request.setStartTime("19:00");
        request.setStudyTagNames(List.of("backend"));
        request.setStudyPlanList(List.of());

        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);
        when(studyRepository.findAllStudyTagByName(List.of("backend"))).thenReturn(List.of(tag));

        studyService.updateStudyApplication(1, 10L, request.toCommand(), null, null);

        verify(study).setExplanation("수정된 상세 소개입니다.");
        verify(study).setStartTime("19:00");
        verify(study).setTags(List.of(tag));
        verify(studyRepository).deleteStudyPlansByStudyId(1);
    }

    @Test
    void allowsUpdatingTheSecondaryMentorAndThumbnailAfterApproval() {
        Study study = mock(Study.class);
        User primaryMentor = mock(User.class);
        User secondaryMentor = mock(User.class);
        MultipartFile thumbnail = mock(MultipartFile.class);
        UpdateStudyRequest request = new UpdateStudyRequest();
        request.setSecondaryMentorId(20L);

        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);
        when(study.getPrimaryMentor()).thenReturn(primaryMentor);
        when(primaryMentor.getId()).thenReturn(10L);
        when(userRepository.findUserById(20L)).thenReturn(Optional.of(secondaryMentor));
        when(secondaryMentor.getUserName()).thenReturn("부멘토");
        when(thumbnail.isEmpty()).thenReturn(false);
        when(filePort.uploadFile(thumbnail)).thenReturn("studies/thumbnails/new.png");
        when(filePort.generatePresignedViewUrl("studies/thumbnails/new.png"))
                .thenReturn(new FileInfo("studies/thumbnails/new.png", "https://example.com/new.png"));

        studyService.updateStudyApplication(1, 10L, request.toCommand(), thumbnail, null);

        verify(study).setSecondaryMentor(secondaryMentor);
        verify(study).setSecondaryMentorName("부멘토");
        verify(study).setThumbnailImage("studies/thumbnails/new.png");
    }

    @Test
    void ignoresDeprecatedGoalWhenUpdatingApplication() {
        Study study = mock(Study.class);
        UpdateStudyRequest request = new UpdateStudyRequest();
        request.setStudyName("수정된 스터디명");
        request.setOneLiner("수정된 한 줄 소개");
        request.setGoal("수정된 목표");
        request.setCapacity(20);
        request.setSelectionCriteria("수정된 선발 기준");
        request.setReferences(List.of());

        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);

        studyService.updateStudyApplication(1, 10L, request.toCommand(), null, List.of());

        verify(study).setStudyName("수정된 스터디명");
        verify(study).setOneLiner("수정된 한 줄 소개");
        verify(study, never()).setGoal("수정된 목표");
        verify(study).setCapacity(20);
        verify(study).setSelectionCriteria("수정된 선발 기준");
        verify(studyRepository).findStudyReferencesByStudyId(1);
    }

    @Test
    void excludesAutonomousStudiesFromApplicationEndpoints() {
        Study study = mock(Study.class);
        when(study.isAutonomousStudy()).thenReturn(true);
        when(studyRepository.findStudyApplicationsByMentorId(10L, 2026, 1)).thenReturn(List.of(study));
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 1));

        assertThat(studyService.getMyStudyApplications(10L)).isEmpty();

        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(study.isMentor(10L)).thenReturn(true);
        assertThatThrownBy(() -> studyService.getMyStudyApplication(10L, 1))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INSUFFICIENT_PERMISSION));

        assertThatThrownBy(() -> studyService.updateStudyApplication(1, 10L, new UpdateStudyRequest().toCommand(), null, null))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AUTONOMOUS_STUDY_APPLICATION_NOT_ALLOWED));
    }

    @Test
    void keepsApprovedApplicationCancellationBlockedBeforeCheckingPeriod() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyById(1)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);

        assertThatThrownBy(() -> studyService.cancelStudyApplication(1, 10L))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.STUDY_ALREADY_APPROVED));

        verify(semesterPhaseGuard, never())
                .requireOpen(org.forif_backend.domain.semester.SemesterPhase.MENTOR_RECRUIT);
    }

    @Test
    void doesNotExposeCancellationWhenApplicationHasDependents() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyApplicationsByMentorId(10L, 2026, 1)).thenReturn(List.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.PENDING);
        when(study.getActYear()).thenReturn(2026);
        when(study.getActSemester()).thenReturn(1);
        when(study.getTags()).thenReturn(List.of());
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 1));
        when(semesterPhaseGuard.isOpen(
                org.forif_backend.domain.semester.SemesterPhase.MENTOR_RECRUIT, 2026, 1))
                .thenReturn(true);
        when(userApplyRepository.existsByStudyId(0)).thenReturn(true);

        boolean canCancel = studyService.getMyStudyApplications(10L).get(0).isCanCancel();

        assertThat(canCancel).isFalse();
        verify(userApplyRepository).existsByStudyId(0);
    }

    @Test
    void exposesCurrentSemesterApprovedApplicationAsModifiableBeforeMenteeRecruitmentStarts() {
        Study study = mock(Study.class);
        when(studyRepository.findStudyApplicationsByMentorId(10L, 2026, 1)).thenReturn(List.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);
        when(study.getActYear()).thenReturn(2026);
        when(study.getActSemester()).thenReturn(1);
        when(study.getTags()).thenReturn(List.of());
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 1));
        when(semesterPhaseGuard.isBeforeStart(
                org.forif_backend.domain.semester.SemesterPhase.MENTEE_RECRUIT, 2026, 1))
                .thenReturn(true);

        var application = studyService.getMyStudyApplications(10L).get(0);

        assertThat(application.isCanModify()).isTrue();
        assertThat(application.isCanCancel()).isFalse();
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

        studyService.updateStudyApplication(1, 10L, request.toCommand(), null, null);

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

        studyService.updateStudyApplication(1, 10L, request.toCommand(), null, null);

        verify(study).setExplanation(request.getExplanation());
        verify(studyRepository, never()).deleteStudyPlansByStudyId(1);
        verify(studyRepository, never()).saveAllStudyPlan(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void removesSecondaryMentorWhenThePatchExplicitlySendsNull() {
        Study study = mock(Study.class);
        UpdateStudyRequest request = new UpdateStudyRequest();
        request.setSecondaryMentorId(null);

        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.PENDING);

        studyService.updateStudyApplication(1, 10L, request.toCommand(), null, null);

        verify(study).setSecondaryMentor(null);
        verify(study).setSecondaryMentorName(null);
    }

    @Test
    void keepsExistingFileReferencesWhenLegacyAdminRequestOmitsRetainedReferenceIds() {
        Study study = mock(Study.class);
        StudyReference fileReference = mock(StudyReference.class);
        UUID referenceId = UUID.randomUUID();
        UpdateStudyRequest request = new UpdateStudyRequest();
        UpdateStudyRequest.Reference fileReferenceRequest = new UpdateStudyRequest.Reference();
        fileReferenceRequest.setType(ReferenceType.FILE);
        fileReferenceRequest.setUrl("https://files.example.com/temporary-view-url");
        request.setReferences(List.of(fileReferenceRequest));

        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(studyRepository.findStudyReferencesByStudyId(1)).thenReturn(List.of(fileReference));
        when(fileReference.getId()).thenReturn(referenceId);
        when(fileReference.getReferenceType()).thenReturn(ReferenceType.FILE);

        studyService.updateStudy(1, request.toCommand());

        verify(studyRepository, never()).deleteStudyReferencesByIds(anyList());
        verify(studyRepository, never()).saveAllStudyReference(anyList());
    }

    @Test
    void preventsRenamingAnAutonomousStudy() {
        Study study = mock(Study.class);
        UpdateStudyRequest request = new UpdateStudyRequest();
        request.setStudyName("변경된 스터디 이름");

        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(study.isAutonomousStudy()).thenReturn(true);

        assertThatThrownBy(() -> studyService.updateStudy(1, request.toCommand()))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AUTONOMOUS_STUDY_NAME_NOT_CHANGEABLE));

        verify(study, never()).setStudyName("변경된 스터디 이름");
    }

    @Test
    void preventsRenamingARegularStudyToTheReservedAutonomousStudyName() {
        Study study = mock(Study.class);
        UpdateStudyRequest request = new UpdateStudyRequest();
        request.setStudyName(Study.AUTONOMOUS_STUDY_NAME);

        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(study.isAutonomousStudy()).thenReturn(false);

        assertThatThrownBy(() -> studyService.updateStudy(1, request.toCommand()))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AUTONOMOUS_STUDY_NAME_RESERVED));

        verify(study, never()).setStudyName(Study.AUTONOMOUS_STUDY_NAME);
    }

    @Test
    void preventsUsingTheReservedNameWhenCreatingAStudyApplication() {
        CreateStudyApplyRequest request = new CreateStudyApplyRequest();
        request.setTitle(Study.AUTONOMOUS_STUDY_NAME);

        assertThatThrownBy(() -> studyService.createStudyApply(10L, request.toCommand(), null, null))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AUTONOMOUS_STUDY_NAME_RESERVED));

        verify(userRepository, never()).findUserById(10L);
    }

    @Test
    void preventsUsingTheReservedNameWhenReapplyingForAStudy() {
        CreateStudyApplyRequest request = new CreateStudyApplyRequest();
        request.setTitle(Study.AUTONOMOUS_STUDY_NAME);

        assertThatThrownBy(() -> studyService.reApplyStudy(1, 10L, request.toCommand(), null, null))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AUTONOMOUS_STUDY_NAME_RESERVED));

        verify(studyRepository, never()).findStudyByIdWithTags(1);
    }

    @Test
    void rejectsCreatingAnotherAutonomousStudyForTheActiveSemester() {
        when(semesterService.getActiveForUpdate()).thenReturn(SemesterInfo.of(2026, 2));
        when(studyRepository.existsByActYearAndActSemesterAndAutonomousFlagTrue(2026, 2)).thenReturn(true);

        assertThatThrownBy(() -> studyService.createAutonomousStudy(10L))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AUTONOMOUS_STUDY_ALREADY_EXISTS));

        verify(studyRepository, never()).saveStudy(org.mockito.ArgumentMatchers.any(Study.class));
    }

    @Test
    void preventsCreatingAnAutonomousStudyAfterMenteeRecruitmentEnds() {
        when(semesterService.getActiveForUpdate()).thenReturn(SemesterInfo.of(2026, 2));
        doThrow(new ForifException(ErrorCode.SEMESTER_PHASE_CLOSED))
                .when(semesterPhaseGuard)
                .requireNotEnded(org.forif_backend.domain.semester.SemesterPhase.MENTEE_RECRUIT, 2026, 2);

        assertThatThrownBy(() -> studyService.createAutonomousStudy(10L))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.SEMESTER_PHASE_CLOSED));

        verify(studyRepository, never()).existsByActYearAndActSemesterAndAutonomousFlagTrue(2026, 2);
    }

    @Test
    void replacesExistingFileReferencesWhenMentorMultipartRequestOmitsRetainedReferenceIds() {
        Study study = mock(Study.class);
        StudyReference existingFileReference = mock(StudyReference.class);
        UUID existingReferenceId = UUID.randomUUID();
        UpdateStudyRequest request = new UpdateStudyRequest();
        request.setReferences(List.of());

        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(study.getStudyStatus()).thenReturn(StudyStatus.PENDING);
        when(studyRepository.findStudyReferencesByStudyId(1)).thenReturn(List.of(existingFileReference));
        when(existingFileReference.getId()).thenReturn(existingReferenceId);
        when(existingFileReference.getReferenceType()).thenReturn(ReferenceType.FILE);
        when(existingFileReference.getContent()).thenReturn("studies/references/old.pdf");
        TransactionSynchronizationManager.initSynchronization();

        studyService.updateStudyApplication(1, 10L, request.toCommand(), null, List.of());

        verify(studyRepository).deleteStudyReferencesByIds(List.of(existingReferenceId));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(filePort).deleteFile("studies/references/old.pdf");
    }

    @Test
    void deletesReplacedReferenceFilesAfterReapplying() {
        Study study = mock(Study.class);
        User primaryMentor = mock(User.class);
        StudyReference existingFileReference = mock(StudyReference.class);
        MultipartFile replacementFile = mock(MultipartFile.class);
        MultipartFile replacementThumbnail = mock(MultipartFile.class);
        CreateStudyApplyRequest request = new CreateStudyApplyRequest();
        CreateStudyApplyRequest.Reference replacementReference = new CreateStudyApplyRequest.Reference();
        replacementReference.setType(ReferenceType.FILE);
        replacementReference.setFileName("replacement.pdf");
        request.setReferences(List.of(replacementReference));

        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(study.getPrimaryMentor()).thenReturn(primaryMentor);
        when(study.getStudyStatus()).thenReturn(StudyStatus.REJECTED);
        when(primaryMentor.getId()).thenReturn(10L);
        when(study.getThumbnailImage()).thenReturn("studies/thumbnails/rejected.png");
        when(studyRepository.findStudyReferencesByStudyId(1)).thenReturn(List.of(existingFileReference));
        when(existingFileReference.getReferenceType()).thenReturn(ReferenceType.FILE);
        when(existingFileReference.getContent()).thenReturn("studies/references/rejected.pdf");
        when(replacementFile.getOriginalFilename()).thenReturn("replacement.pdf");
        when(filePort.uploadFile(replacementFile)).thenReturn("studies/references/replacement.pdf");
        when(filePort.generatePresignedViewUrl("studies/references/replacement.pdf"))
                .thenReturn(new FileInfo("studies/references/replacement.pdf", "https://example.com/replacement.pdf"));
        when(replacementThumbnail.isEmpty()).thenReturn(false);
        when(filePort.uploadFile(replacementThumbnail)).thenReturn("studies/thumbnails/replacement.png");
        when(filePort.generatePresignedViewUrl("studies/thumbnails/replacement.png"))
                .thenReturn(new FileInfo("studies/thumbnails/replacement.png", "https://example.com/replacement.png"));
        TransactionSynchronizationManager.initSynchronization();

        studyService.reApplyStudy(1, 10L, request.toCommand(), replacementThumbnail, List.of(replacementFile));

        verify(studyRepository).deleteStudyReferencesByStudyId(1);
        verify(filePort).uploadFile(replacementFile);
        verify(filePort).uploadFile(replacementThumbnail);
        verify(filePort, never()).deleteFile("studies/references/rejected.pdf");
        verify(filePort, never()).deleteFile("studies/thumbnails/rejected.png");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

        verify(filePort).deleteFile("studies/references/rejected.pdf");
        verify(filePort).deleteFile("studies/thumbnails/rejected.png");
        verify(filePort, never()).deleteFile("studies/references/replacement.pdf");
        verify(filePort, never()).deleteFile("studies/thumbnails/replacement.png");
    }

    @Test
    void preservesRetainedReferenceIdAndDeletesOnlyRemovedFileAfterCommit() {
        Study study = mock(Study.class);
        StudyReference retainedReference = mock(StudyReference.class);
        StudyReference removedFileReference = mock(StudyReference.class);
        UUID retainedId = UUID.randomUUID();
        UUID removedId = UUID.randomUUID();
        UpdateStudyRequest request = new UpdateStudyRequest();
        request.setReferences(List.of());
        request.setRetainedReferenceIds(List.of(retainedId));

        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(studyRepository.findStudyReferencesByStudyId(1))
                .thenReturn(List.of(retainedReference, removedFileReference));
        when(retainedReference.getId()).thenReturn(retainedId);
        when(removedFileReference.getId()).thenReturn(removedId);
        when(removedFileReference.getReferenceType()).thenReturn(ReferenceType.FILE);
        when(removedFileReference.getContent()).thenReturn("studies/references/old.pdf");
        TransactionSynchronizationManager.initSynchronization();

        studyService.updateStudy(1, request.toCommand());

        verify(studyRepository).deleteStudyReferencesByIds(List.of(removedId));
        verify(studyRepository, never()).saveAllStudyReference(anyList());
        verify(filePort, never()).deleteFile("studies/references/old.pdf");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

        verify(filePort).deleteFile("studies/references/old.pdf");
    }

}
