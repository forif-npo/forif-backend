package org.forif_backend.infrastructure.persistence.study;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.Study;
import java.util.List;
import org.forif_backend.domain.study.StudyStatus;
import org.forif_backend.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class StudyJpaRepositoryRecruitStatusTest {

    @Autowired private EntityManager entityManager;
    @Autowired private StudyJpaRepository studyJpaRepository;

    @Test
    void closesOnlyEstablishedStudiesOutsideTheActiveSemester() {
        User mentor = persistUser(900020L);
        Study priorApproved = persistStudy(mentor, 2026, 1, StudyStatus.APPROVED, RecruitStatus.APPLICABLE, "지난 학기");
        Study activeApproved = persistStudy(mentor, 2026, 2, StudyStatus.APPROVED, RecruitStatus.APPLICABLE, "활동 학기");
        Study priorPending = persistStudy(mentor, 2025, 2, StudyStatus.PENDING, RecruitStatus.APPLICABLE, "대기 스터디");
        Study priorStarted = persistStudy(mentor, 2025, 1, StudyStatus.STARTED, RecruitStatus.APPLICABLE, "지난 개설 스터디");
        Study alreadyClosed = persistStudy(mentor, 2025, 1, StudyStatus.APPROVED, RecruitStatus.CLOSED, "이미 마감");
        entityManager.flush();
        entityManager.clear();

        int changed = studyJpaRepository.closeRecruitmentForNonActiveStudies(
                2026, 2, RecruitStatus.CLOSED, List.of(StudyStatus.APPROVED, StudyStatus.STARTED));

        assertThat(changed).isEqualTo(2);
        entityManager.clear();
        assertThat(findRecruitStatus(priorApproved)).isEqualTo(RecruitStatus.CLOSED);
        assertThat(findRecruitStatus(activeApproved)).isEqualTo(RecruitStatus.APPLICABLE);
        assertThat(findRecruitStatus(priorPending)).isEqualTo(RecruitStatus.APPLICABLE);
        assertThat(findRecruitStatus(priorStarted)).isEqualTo(RecruitStatus.CLOSED);
        assertThat(findRecruitStatus(alreadyClosed)).isEqualTo(RecruitStatus.CLOSED);
    }

    private User persistUser(Long id) {
        User user = User.createUser(id, "멘토", id + "@forif.org", "010-0000-0000", "컴퓨터공학과");
        entityManager.persist(user);
        return user;
    }

    private Study persistStudy(
            User mentor, int year, int semester, StudyStatus status, RecruitStatus recruitStatus, String name) {
        Study study = Study.createPendingStudy(mentor, year, semester);
        study.setStudyName(name);
        study.setRecruitStatus(recruitStatus);
        if (status == StudyStatus.APPROVED || status == StudyStatus.STARTED) {
            study.approve();
        }
        if (status == StudyStatus.STARTED) {
            study.start();
        }
        entityManager.persist(study);
        return study;
    }

    private RecruitStatus findRecruitStatus(Study study) {
        return entityManager.find(Study.class, study.getId()).getRecruitStatus();
    }
}
