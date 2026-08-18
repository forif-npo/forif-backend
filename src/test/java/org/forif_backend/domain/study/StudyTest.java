package org.forif_backend.domain.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.forif_backend.domain.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StudyTest {

    @Test
    void createsAutonomousStudyAsApprovedWithTheCreatingAdminAsPrimaryMentor() {
        User admin = Mockito.mock(User.class);
        given(admin.getUserName()).willReturn("운영진");

        Study study = Study.createAutonomousStudy(admin, 2026, 2);

        assertThat(study.getActYear()).isEqualTo(2026);
        assertThat(study.getActSemester()).isEqualTo(2);
        assertThat(study.getStudyName()).isEqualTo(Study.AUTONOMOUS_STUDY_NAME);
        assertThat(study.getStudyStatus()).isEqualTo(StudyStatus.APPROVED);
        assertThat(study.getPrimaryMentor()).isSameAs(admin);
        assertThat(study.getPrimaryMentorName()).isEqualTo("운영진");
        assertThat(study.getOneLiner()).isNotBlank();
        assertThat(study.getExplanation()).isNotBlank();
        assertThat(study.isAutonomousStudy()).isTrue();
    }

    @Test
    void identifiesAnAutonomousStudyByTheDedicatedFlagInsteadOfItsName() {
        User mentor = Mockito.mock(User.class);
        given(mentor.getUserName()).willReturn("멘토");

        Study regularStudy = Study.createPendingStudy(mentor, 2026, 2);

        assertThat(regularStudy.isAutonomousStudy()).isFalse();
    }
}
