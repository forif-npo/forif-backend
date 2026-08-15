package org.forif_backend.application.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;
import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.study.dto.MentorConfirmationStatusResult;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.study.MentorConfirmation;
import org.forif_backend.domain.study.MentorConfirmationRepository;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyStatus;
import org.junit.jupiter.api.Test;

class MentorConfirmationServiceTest {

    @Test
    void doesNotIssueWhenTheActivityStartDateIsAfterTheEndDate() {
        StudyRepository studyRepository = mock(StudyRepository.class);
        SemesterService semesterService = mock(SemesterService.class);
        MentorConfirmationService service = new MentorConfirmationService(
                studyRepository,
                mock(MentorConfirmationRepository.class),
                semesterService,
                mock(StaffAccountRepository.class),
                mock(CertificateImageGenerator.class),
                mock(FilePort.class)
        );
        Study study = mock(Study.class);

        when(studyRepository.findStudyById(1)).thenReturn(Optional.of(study));
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(study.getActYear()).thenReturn(2026);
        when(study.getActSemester()).thenReturn(1);
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);

        assertThatThrownBy(() -> service.issueConfirmations(
                1, List.of(), "2026.08.15.~2026.02.01."))
                .isInstanceOf(ForifException.class);
    }

    @Test
    void doesNotReturnAConfirmationForAnUnapprovedStudy() {
        StudyRepository studyRepository = mock(StudyRepository.class);
        SemesterService semesterService = mock(SemesterService.class);
        MentorConfirmationService service = new MentorConfirmationService(
                studyRepository,
                mock(MentorConfirmationRepository.class),
                semesterService,
                mock(StaffAccountRepository.class),
                mock(CertificateImageGenerator.class),
                mock(FilePort.class)
        );
        Study study = mock(Study.class);

        when(studyRepository.findStudyById(1)).thenReturn(Optional.of(study));
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(study.getActYear()).thenReturn(2026);
        when(study.getActSemester()).thenReturn(1);
        when(study.getStudyStatus()).thenReturn(StudyStatus.PENDING);

        assertThatThrownBy(() -> service.getMyConfirmation(1, 100L))
                .isInstanceOf(ForifException.class);
    }

    @Test
    void returnsAFreshDownloadUrlForAnIssuedPreviousSemesterConfirmation() {
        StudyRepository studyRepository = mock(StudyRepository.class);
        MentorConfirmationRepository confirmationRepository = mock(MentorConfirmationRepository.class);
        SemesterService semesterService = mock(SemesterService.class);
        StaffAccountRepository staffAccountRepository = mock(StaffAccountRepository.class);
        CertificateImageGenerator imageGenerator = mock(CertificateImageGenerator.class);
        FilePort filePort = mock(FilePort.class);
        MentorConfirmationService service = new MentorConfirmationService(
                studyRepository,
                confirmationRepository,
                semesterService,
                staffAccountRepository,
                imageGenerator,
                filePort
        );
        Study study = mock(Study.class);
        MentorConfirmation confirmation = mock(MentorConfirmation.class);
        String objectKey = "mentor-confirmations/2026-1/1/100.png";
        String viewUrl = "https://example.com/fresh-confirmation-url";

        when(studyRepository.findStudyById(1)).thenReturn(Optional.of(study));
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(study.getActYear()).thenReturn(2026);
        when(study.getActSemester()).thenReturn(1);
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);
        when(study.isMentor(100L)).thenReturn(true);
        when(confirmationRepository.findByStudyIdAndMentorId(1, 100L))
                .thenReturn(Optional.of(confirmation));
        when(confirmation.getConfirmationObjectKey()).thenReturn(objectKey);
        when(filePort.generatePresignedViewUrl(objectKey)).thenReturn(new FileInfo(objectKey, viewUrl));

        MentorConfirmationStatusResult result = service.getMyConfirmation(1, 100L);

        assertThat(result.issued()).isTrue();
        assertThat(result.confirmationUrl()).isEqualTo(viewUrl);
    }
}
