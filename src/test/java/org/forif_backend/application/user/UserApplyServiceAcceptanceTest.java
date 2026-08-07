package org.forif_backend.application.user;

import org.forif_backend.application.dues.DuesService;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.study.StudyMentorAccess;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApplyServiceAcceptanceTest {

    @Mock
    private SemesterService semesterService;
    @Mock
    private SemesterPhaseGuard semesterPhaseGuard;
    @Mock
    private StudyMentorAccess studyMentorAccess;
    @Mock
    private DuesService duesService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudyRepository studyRepository;
    @Mock
    private StudyUserRepository studyUserRepository;

    @InjectMocks
    private UserApplyService userApplyService;

    @Test
    void acceptsApplicationWithoutDirectlyCreatingStudyUser() {
        Study study = mock(Study.class);
        User applicant = User.createUser(1L, "신청자", "applicant@hanyang.ac.kr", "01011112222", "컴퓨터학부");
        UserApply application = mock(UserApply.class);

        when(studyRepository.findStudyById(10)).thenReturn(Optional.of(study));
        when(userRepository.findUserApplyById(100L)).thenReturn(application);
        when(application.getPrimaryStudy()).thenReturn(10);
        when(application.getApplier()).thenReturn(applicant);

        userApplyService.acceptApplications(99L, 10, List.of(100L));

        verify(application).updateStatus(10, org.forif_backend.domain.user.UserApplyStatus.ACCEPT);
        verify(duesService).ensureMemberCheck(study, applicant);
        verify(duesService).registerStudyUserIfEligible(study, applicant);
        verify(studyUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
