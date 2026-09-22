package org.forif_backend.application.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import org.forif_backend.application.dues.DuesService;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.user.UserApplyService;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.study.MentorConfirmationRepository;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyAttendanceRepository;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.UserApplyRepository;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;

class AutonomousStudyVisibilityTest {

    @Test
    void excludesAutonomousStudiesFromCreatedAndEnrolledStudyLists() {
        StudyRepository studyRepository = mock(StudyRepository.class);
        StudyUserRepository studyUserRepository = mock(StudyUserRepository.class);
        SemesterService semesterService = mock(SemesterService.class);
        StudyService service = new StudyService(
                semesterService,
                mock(SemesterPhaseGuard.class),
                mock(StudyRecruitStatusPolicy.class),
                mock(StudyMentorAccess.class),
                studyRepository,
                studyUserRepository,
                mock(StudyAttendanceRepository.class),
                mock(UserRepository.class),
                mock(UserApplyRepository.class),
                mock(FilePort.class),
                mock(StaffAccountService.class),
                mock(StaffAccountRepository.class),
                mock(MentorConfirmationRepository.class)
        );
        Study regularStudy = mock(Study.class);
        when(regularStudy.isAutonomousStudy()).thenReturn(false);
        when(regularStudy.getId()).thenReturn(100);
        when(regularStudy.getActYear()).thenReturn(2099);
        when(regularStudy.getActSemester()).thenReturn(1);
        when(regularStudy.getTags()).thenReturn(List.of());

        Study autonomousStudy = mock(Study.class);
        when(autonomousStudy.isAutonomousStudy()).thenReturn(true);
        when(studyRepository.findStudiesByMentorId(999L))
                .thenReturn(List.of(regularStudy, autonomousStudy));
        when(studyRepository.findStudiesByUserId(999L))
                .thenReturn(List.of(regularStudy, autonomousStudy));
        when(studyUserRepository.findAllByUserId(999L)).thenReturn(List.of());
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2099, 1));

        assertThat(service.getMyCreatedStudies(999L))
                .extracting(study -> study.getId())
                .containsExactly(100);
        assertThat(service.getUserStudies(999L).semesters())
                .extracting(semester -> semester.study().studyId())
                .containsExactly(100);
    }

    @Test
    void rejectsApplicantLookupForAutonomousStudies() {
        StudyRepository studyRepository = mock(StudyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserApplyService service = new UserApplyService(
                mock(SemesterService.class),
                mock(SemesterPhaseGuard.class),
                mock(StudyMentorAccess.class),
                mock(DuesService.class),
                userRepository,
                mock(UserApplyRepository.class),
                studyRepository,
                mock(StudyUserRepository.class),
                mock(Validator.class)
        );
        Study autonomousStudy = mock(Study.class);
        when(autonomousStudy.isAutonomousStudy()).thenReturn(true);
        when(studyRepository.findStudyById(101)).thenReturn(Optional.of(autonomousStudy));

        assertThatThrownBy(() -> service.getApplyInfo(999L, 101, 0, 20, null, SortDirection.DESC))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AUTONOMOUS_STUDY_APPLICATION_DECISION_NOT_ALLOWED));

        verifyNoInteractions(userRepository);
    }
}
