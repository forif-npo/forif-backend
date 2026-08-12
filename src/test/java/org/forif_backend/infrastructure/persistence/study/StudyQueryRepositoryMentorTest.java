package org.forif_backend.infrastructure.persistence.study;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, StudyQueryRepository.class})
class StudyQueryRepositoryMentorTest {

    private static final int YEAR = 2026;
    private static final int SEMESTER = 1;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private StudyQueryRepository studyQueryRepository;

    @Test
    void returnsOnlyApprovedMentorsAndSupportsStudyNameSearch() {
        User approvedMentor = persistUser(900001L, "승인 멘토");
        User pendingMentor = persistUser(900002L, "대기 멘토");
        User rejectedSecondaryMentor = persistUser(900003L, "반려 부멘토");

        persistApprovedStudy(approvedMentor, "운영체제 심화 스터디");
        persistPendingStudy(pendingMentor, "운영체제 대기 스터디");
        persistRejectedStudy(pendingMentor, rejectedSecondaryMentor, "운영체제 반려 스터디");
        entityManager.flush();
        entityManager.clear();

        List<User> mentors = studyQueryRepository.searchMentors(null, 20, "운영체제");
        List<User> offsetMentors = studyQueryRepository.searchMentorsWithOffset(0, 20, "운영체제");
        long mentorCount = studyQueryRepository.countMentors("운영체제");
        List<User> semesterMentors = studyQueryRepository.searchMentorsByYearSemester(
                YEAR,
                SEMESTER,
                null,
                20,
                "운영체제"
        );
        List<User> semesterOffsetMentors = studyQueryRepository.searchMentorsByYearSemesterWithOffset(
                YEAR,
                SEMESTER,
                0,
                20,
                "운영체제"
        );
        long semesterMentorCount = studyQueryRepository.countMentorsByYearSemester(
                YEAR,
                SEMESTER,
                "운영체제"
        );
        Set<Long> mentorIds = studyQueryRepository.findMentorUserIdsByUserIds(
                List.of(approvedMentor.getId(), pendingMentor.getId(), rejectedSecondaryMentor.getId()),
                YEAR,
                SEMESTER
        );
        Map<Long, String> studyNames = studyQueryRepository.findMentorStudyNamesByUserIds(
                List.of(approvedMentor.getId(), pendingMentor.getId(), rejectedSecondaryMentor.getId()),
                YEAR,
                SEMESTER
        );

        assertThat(mentors).extracting(User::getId).containsExactly(approvedMentor.getId());
        assertThat(offsetMentors).extracting(User::getId).containsExactly(approvedMentor.getId());
        assertThat(mentorCount).isEqualTo(1);
        assertThat(semesterMentors).extracting(User::getId).containsExactly(approvedMentor.getId());
        assertThat(semesterOffsetMentors).extracting(User::getId).containsExactly(approvedMentor.getId());
        assertThat(semesterMentorCount).isEqualTo(1);
        assertThat(mentorIds).containsExactly(approvedMentor.getId());
        assertThat(studyNames).containsExactly(Map.entry(approvedMentor.getId(), "운영체제 심화 스터디"));
    }

    private User persistUser(Long id, String name) {
        User user = User.createUser(id, name, id + "@forif.org", "010-0000-0000", "컴퓨터공학과");
        entityManager.persist(user);
        return user;
    }

    private void persistApprovedStudy(User mentor, String name) {
        Study study = Study.createPendingStudy(mentor, YEAR, SEMESTER);
        study.setStudyName(name);
        study.approve();
        entityManager.persist(study);
    }

    private void persistPendingStudy(User mentor, String name) {
        Study study = Study.createPendingStudy(mentor, YEAR, SEMESTER);
        study.setStudyName(name);
        entityManager.persist(study);
    }

    private void persistRejectedStudy(User primaryMentor, User secondaryMentor, String name) {
        Study study = Study.createPendingStudy(primaryMentor, YEAR, SEMESTER);
        study.setStudyName(name);
        study.setSecondaryMentor(secondaryMentor);
        study.setSecondaryMentorName(secondaryMentor.getUserName());
        study.reject("반려 사유");
        entityManager.persist(study);
    }
}
