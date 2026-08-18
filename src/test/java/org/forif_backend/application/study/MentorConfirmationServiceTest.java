package org.forif_backend.application.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;
import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.study.dto.MentorConfirmationStatusResult;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccount;
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
                .isInstanceOf(ForifException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MENTOR_CONFIRMATION_INVALID_ACTIVITY_PERIOD);
    }

    @Test
    void returnsAFormatErrorForAnInvalidActivityPeriod() {
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

        assertThatThrownBy(() -> service.issueConfirmations(1, List.of(), "2026-02-01~2026-08-15"))
                .isInstanceOf(ForifException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MENTOR_CONFIRMATION_INVALID_ACTIVITY_PERIOD_FORMAT);
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
    void returnsAFreshDownloadUrlForAFormerMentorWithIssuedConfirmation() {
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
        when(study.isMentor(100L)).thenReturn(false);
        when(confirmationRepository.findByStudyIdAndMentorId(1, 100L))
                .thenReturn(Optional.of(confirmation));
        when(confirmation.getConfirmationObjectKey()).thenReturn(objectKey);
        when(filePort.generatePresignedViewUrl(objectKey)).thenReturn(new FileInfo(objectKey, viewUrl));

        MentorConfirmationStatusResult result = service.getMyConfirmation(1, 100L);

        assertThat(result.issued()).isTrue();
        assertThat(result.confirmationUrl()).isEqualTo(viewUrl);
    }

    @Test
    void allowsAdminsToCheckAnIssuedStatusWithoutCurrentMentorMembership() {
        StudyRepository studyRepository = mock(StudyRepository.class);
        MentorConfirmationRepository confirmationRepository = mock(MentorConfirmationRepository.class);
        SemesterService semesterService = mock(SemesterService.class);
        MentorConfirmationService service = new MentorConfirmationService(
                studyRepository,
                confirmationRepository,
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
        when(confirmationRepository.findByStudyIdAndMentorId(1, 100L)).thenReturn(Optional.empty());

        MentorConfirmationStatusResult result = service.getConfirmationForAdmin(1, 100L);

        assertThat(result.issued()).isFalse();
    }

    @Test
    void upsertsConfirmationSoConcurrentIssuanceDoesNotCreateDuplicateRows() {
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
        org.forif_backend.domain.user.User mentor = mock(org.forif_backend.domain.user.User.class);
        StaffAccount president = mock(StaffAccount.class);

        when(studyRepository.findStudyById(1)).thenReturn(Optional.of(study));
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(study.getId()).thenReturn(1);
        when(study.getActYear()).thenReturn(2026);
        when(study.getActSemester()).thenReturn(1);
        when(study.getStudyStatus()).thenReturn(StudyStatus.APPROVED);
        when(study.getStudyName()).thenReturn("테스트 스터디");
        when(study.getPrimaryMentor()).thenReturn(mentor);
        when(study.getSecondaryMentor()).thenReturn(null);
        when(mentor.getId()).thenReturn(100L);
        when(mentor.getUserName()).thenReturn("멘토");
        when(mentor.getDepartment()).thenReturn("컴퓨터공학부");
        when(staffAccountRepository.findByAffiliation("회장")).thenReturn(List.of(president));
        when(president.getSignatureObjectKey()).thenReturn("signatures/president.png");
        when(president.getName()).thenReturn("회장");
        when(filePort.downloadBytes("signatures/president.png")).thenReturn(new byte[]{1});
        when(imageGenerator.generateMentorConfirmation(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(byte[].class)))
                .thenReturn(new byte[]{1});
        when(filePort.uploadBytes(org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn("mentor-confirmations/2026-1/1/100.png");
        when(filePort.generatePresignedViewUrl("mentor-confirmations/2026-1/1/100.png"))
                .thenReturn(new FileInfo("mentor-confirmations/2026-1/1/100.png", "https://example.com/confirmation"));

        service.issueConfirmations(1, List.of(100L), "2026.02.01.~2026.08.15.");

        verify(confirmationRepository).upsert(1, 100L, "mentor-confirmations/2026-1/1/100.png");
    }
}
