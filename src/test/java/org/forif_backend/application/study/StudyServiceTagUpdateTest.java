package org.forif_backend.application.study;

import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyTag;
import org.forif_backend.domain.study.StudyUserRepository;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.study.dto.UpdateStudyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudyServiceTagUpdateTest {

    private final StudyRepository studyRepository = mock(StudyRepository.class);
    private StudyService studyService;

    @BeforeEach
    void setUp() {
        studyService = new StudyService(
                mock(SemesterService.class),
                mock(SemesterPhaseGuard.class),
                mock(StudyRecruitStatusPolicy.class),
                mock(StudyMentorAccess.class),
                studyRepository,
                mock(StudyUserRepository.class),
                mock(UserRepository.class),
                mock(FilePort.class),
                mock(StaffAccountService.class),
                mock(StaffAccountRepository.class)
        );
    }

    @Test
    void clearsAllTagsWhenAnEmptyTagIdListIsProvided() {
        Study study = mock(Study.class);
        UpdateStudyRequest request = new UpdateStudyRequest();
        request.setStudyTagIds(List.of());
        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));

        studyService.updateStudy(1, request);

        verify(study).setTags(List.of());
    }

    @Test
    void resolvesCaseVariantsAsOneTag() {
        Study study = mock(Study.class);
        StudyTag tag = mock(StudyTag.class);
        UpdateStudyRequest request = new UpdateStudyRequest();
        request.setStudyTagNames(List.of(" AI ", "ai"));
        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));
        when(studyRepository.findAllStudyTagByName(List.of("ai"))).thenReturn(List.of(tag));

        studyService.updateStudy(1, request);

        verify(studyRepository).findAllStudyTagByName(List.of("ai"));
        verify(study).setTags(List.of(tag));
    }

    @Test
    void rejectsNullTagNamesInsteadOfThrowingNullPointerException() {
        Study study = mock(Study.class);
        UpdateStudyRequest request = new UpdateStudyRequest();
        request.setStudyTagNames(Arrays.asList("ai", null));
        when(studyRepository.findStudyByIdWithTags(1)).thenReturn(Optional.of(study));

        assertThatThrownBy(() -> studyService.updateStudy(1, request))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
