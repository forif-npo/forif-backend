package org.forif_backend.application.study;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.hackathon.HackathonRepository;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyAttendanceRepository;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyStatus;
import org.forif_backend.domain.study.StudyUserRepository;
import org.junit.jupiter.api.Test;

class AutonomousStudyOperationTest {

    @Test
    void blocksAttendanceForAnAutonomousStudy() {
        StudyRepository studyRepository = mock(StudyRepository.class);
        Study study = mock(Study.class);
        StudyUserRepository studyUserRepository = mock(StudyUserRepository.class);
        StudyAttendanceRepository attendanceRepository = mock(StudyAttendanceRepository.class);
        StudyAttendanceService service = new StudyAttendanceService(
                studyRepository,
                mock(StudyMentorAccess.class),
                studyUserRepository,
                attendanceRepository
        );
        when(studyRepository.findStudyById(1)).thenReturn(Optional.of(study));
        when(study.isAutonomousStudy()).thenReturn(true);

        assertThatThrownBy(() -> service.getAttendance(10L, 1))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AUTONOMOUS_STUDY_OPERATION_NOT_ALLOWED));

        verify(studyUserRepository, never()).findAllByStudyId(1);
        verify(attendanceRepository, never()).findAllByStudyId(1);
    }

    @Test
    void blocksAttendanceUntilTheStudyHasStarted() {
        StudyRepository studyRepository = mock(StudyRepository.class);
        Study study = mock(Study.class);
        StudyAttendanceService service = new StudyAttendanceService(
                studyRepository,
                mock(StudyMentorAccess.class),
                mock(StudyUserRepository.class),
                mock(StudyAttendanceRepository.class)
        );
        when(studyRepository.findStudyById(1)).thenReturn(Optional.of(study));
        when(study.isAutonomousStudy()).thenReturn(false);
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);

        assertThatThrownBy(() -> service.getAttendance(10L, 1))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void blocksCertificatesForAnAutonomousStudy() {
        StudyRepository studyRepository = mock(StudyRepository.class);
        Study study = mock(Study.class);
        StudyUserRepository studyUserRepository = mock(StudyUserRepository.class);
        CertificateService service = new CertificateService(
                studyRepository,
                studyUserRepository,
                mock(StudyAttendanceRepository.class),
                mock(HackathonRepository.class),
                mock(StaffAccountRepository.class),
                mock(CertificateImageGenerator.class),
                mock(FilePort.class)
        );
        when(studyRepository.findStudyById(1)).thenReturn(Optional.of(study));
        when(study.isAutonomousStudy()).thenReturn(true);

        assertThatThrownBy(() -> service.getCertificateTargets(1))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AUTONOMOUS_STUDY_OPERATION_NOT_ALLOWED));

        verify(studyUserRepository, never()).findAllByStudyId(1);
    }

    @Test
    void blocksManualCertificatesForTheLegacyAutonomousStudyName() {
        CertificateService service = new CertificateService(
                mock(StudyRepository.class),
                mock(StudyUserRepository.class),
                mock(StudyAttendanceRepository.class),
                mock(HackathonRepository.class),
                mock(StaffAccountRepository.class),
                mock(CertificateImageGenerator.class),
                mock(FilePort.class)
        );

        assertThatThrownBy(() -> service.issueManualCertificate(
                "홍길동", "20260001", "컴퓨터공학과", " 자율스터디 ",
                "2026. 03. 01. ~ 2026. 06. 30.", "2026. 07. 01.", "회장"))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.AUTONOMOUS_STUDY_OPERATION_NOT_ALLOWED));
    }
}
