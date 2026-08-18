package org.forif_backend.application.study;

import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyAttendanceRepository;
import org.forif_backend.domain.study.MentorConfirmationRepository;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyStatus;
import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.UserApplyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 승인 전 스터디 개설 취소(FOR-130)의 경계 동작.
 *
 * 취소는 되돌릴 수 없는 삭제이므로, 지원서·수강생·출석 같은 멘티 기록이
 * 하나라도 걸려 있으면 막아야 한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudyServiceCancelApplicationTest {

    private static final Integer STUDY_ID = 100;
    private static final Long MENTOR_ID = 2024000001L;

    @Mock
    private StudyRepository studyRepository;
    @Mock
    private StudyUserRepository studyUserRepository;
    @Mock
    private StudyAttendanceRepository studyAttendanceRepository;
    @Mock
    private UserApplyRepository userApplyRepository;
    @Mock
    private StudyMentorAccess studyMentorAccess;
    @Mock
    private SemesterPhaseGuard semesterPhaseGuard;
    @Mock
    private FilePort filePort;
    @Mock
    private org.forif_backend.application.semester.SemesterService semesterService;
    @Mock
    private StudyRecruitStatusPolicy recruitStatusPolicy;
    @Mock
    private org.forif_backend.domain.user.UserRepository userRepository;
    @Mock
    private org.forif_backend.application.staff.StaffAccountService staffAccountService;
    @Mock
    private org.forif_backend.domain.staff.StaffAccountRepository staffAccountRepository;
    @Mock
    private MentorConfirmationRepository mentorConfirmationRepository;
    @InjectMocks
    private StudyService studyService;

    @Mock
    private Study study;

    @BeforeEach
    void setUp() {
        when(study.getId()).thenReturn(STUDY_ID);
        when(study.getStudyStatus()).thenReturn(StudyStatus.PENDING);
        when(study.getThumbnailImage()).thenReturn(null);
        when(studyRepository.findStudyById(STUDY_ID)).thenReturn(Optional.of(study));
        when(userApplyRepository.existsByStudyId(STUDY_ID)).thenReturn(false);
        when(studyUserRepository.findAllByStudyId(STUDY_ID)).thenReturn(List.of());
        when(studyAttendanceRepository.findAllByStudyId(STUDY_ID)).thenReturn(List.of());
        when(studyRepository.findStudyReferencesByStudyId(STUDY_ID)).thenReturn(List.of());
    }

    @Test
    void 승인_전_스터디는_취소된다() {
        studyService.cancelStudyApplication(STUDY_ID, MENTOR_ID);

        verify(studyRepository).deleteStudyById(STUDY_ID);
    }

    @Test
    void 승인된_스터디는_취소할_수_없다() {
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);

        assertThatThrownBy(() -> studyService.cancelStudyApplication(STUDY_ID, MENTOR_ID))
                .isInstanceOf(ForifException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BAD_REQUEST);
        verify(studyRepository, never()).deleteStudyById(STUDY_ID);
    }

    @Test
    void 지원서가_있으면_취소를_차단한다() {
        when(userApplyRepository.existsByStudyId(STUDY_ID)).thenReturn(true);

        assertThatThrownBy(() -> studyService.cancelStudyApplication(STUDY_ID, MENTOR_ID))
                .isInstanceOf(ForifException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STUDY_CANCEL_HAS_DEPENDENTS);
        verify(studyRepository, never()).deleteStudyById(STUDY_ID);
    }

    @Test
    void 수강생이_있으면_취소를_차단한다() {
        when(studyUserRepository.findAllByStudyId(STUDY_ID)).thenReturn(List.of(mock(StudyUser.class)));

        assertThatThrownBy(() -> studyService.cancelStudyApplication(STUDY_ID, MENTOR_ID))
                .isInstanceOf(ForifException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STUDY_CANCEL_HAS_DEPENDENTS);
        verify(studyRepository, never()).deleteStudyById(STUDY_ID);
    }

    /** 스토리지 장애가 취소 자체를 막으면 안 된다 */
    @Test
    void 파일_삭제가_실패해도_취소는_진행된다() {
        when(study.getThumbnailImage()).thenReturn("studies/thumb.png");
        org.mockito.Mockito.doThrow(new RuntimeException("storage down"))
                .when(filePort).deleteFile("studies/thumb.png");

        studyService.cancelStudyApplication(STUDY_ID, MENTOR_ID);

        verify(studyRepository).deleteStudyById(STUDY_ID);
    }

    @Test
    void 스터디_삭제_전에_멘토_확인서_발급_이력을_삭제한다() {
        studyService.deleteStudy(STUDY_ID);

        InOrder inOrder = inOrder(mentorConfirmationRepository, studyRepository);
        inOrder.verify(mentorConfirmationRepository).deleteByStudyId(STUDY_ID);
        inOrder.verify(studyRepository).deleteStudyById(STUDY_ID);
    }
}
