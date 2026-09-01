package org.forif_backend.domain.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void reservesBothCurrentAndLegacyAutonomousStudyNames() {
        assertThat(Study.isAutonomousStudyName("자율부원")).isTrue();
        assertThat(Study.isAutonomousStudyName("자율스터디")).isTrue();
        assertThat(Study.isAutonomousStudyName("일반 스터디")).isFalse();
    }

    @Test
    void identifiesBothPrimaryAndSecondaryMentorsFromTheStudyRelationship() {
        User primaryMentor = Mockito.mock(User.class);
        User secondaryMentor = Mockito.mock(User.class);
        given(primaryMentor.getId()).willReturn(1L);
        given(primaryMentor.getUserName()).willReturn("대표 멘토");
        given(secondaryMentor.getId()).willReturn(2L);

        Study study = Study.createPendingStudy(primaryMentor, 2026, 2);
        study.setSecondaryMentor(secondaryMentor);

        assertThat(study.isMentor(1L)).isTrue();
        assertThat(study.isMentor(2L)).isTrue();
        assertThat(study.isMentor(3L)).isFalse();
    }

    @Test
    void startsAnApprovedStudyOnlyOnce() {
        User mentor = Mockito.mock(User.class);
        given(mentor.getUserName()).willReturn("멘토");
        Study study = Study.createPendingStudy(mentor, 2026, 2);
        study.approve();

        study.start();

        assertThat(study.getStudyStatus()).isEqualTo(StudyStatus.STARTED);
        assertThatThrownBy(study::start).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(study::approve).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> study.reject("사유")).isInstanceOf(RuntimeException.class);
    }
}
