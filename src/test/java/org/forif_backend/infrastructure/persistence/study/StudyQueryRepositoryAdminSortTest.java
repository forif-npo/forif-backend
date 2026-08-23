package org.forif_backend.infrastructure.persistence.study;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyDifficulty;
import org.forif_backend.domain.study.StudyStatus;
import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, StudyQueryRepository.class})
class StudyQueryRepositoryAdminSortTest {

    private static final int YEAR = 2026;
    private static final int SEMESTER = 1;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private StudyQueryRepository studyQueryRepository;

    @Test
    void ordersAllMatchingStudiesBeforeApplyingTheOffset() {
        User mentor = persistUser(900010L, "정렬 멘토");
        persistApprovedStudy(mentor, "다람쥐");
        persistApprovedStudy(mentor, "가나다");
        persistApprovedStudy(mentor, "나비");
        entityManager.flush();
        entityManager.clear();

        List<SortCriteria> sorting = List.of(new SortCriteria("study_name", SortDirection.ASC));
        List<Study> firstPage = studyQueryRepository.searchAdminStudiesWithOffset(
                0, 2, YEAR, SEMESTER, null, List.of(StudyStatus.APPROVED), sorting);
        List<Study> secondPage = studyQueryRepository.searchAdminStudiesWithOffset(
                1, 2, YEAR, SEMESTER, null, List.of(StudyStatus.APPROVED), sorting);

        assertThat(firstPage).extracting(Study::getStudyName)
                .containsExactly("가나다", "나비");
        assertThat(secondPage).extracting(Study::getStudyName)
                .containsExactly("다람쥐");
    }

    @Test
    void ordersWeekDaysFromMondayThroughSundayAndPutsNullLast() {
        User mentor = persistUser(900011L, "요일 정렬 멘토");
        Study sunday = persistApprovedStudy(mentor, "일요일");
        Study monday = persistApprovedStudy(mentor, "월요일");
        Study saturday = persistApprovedStudy(mentor, "토요일");
        persistApprovedStudy(mentor, "요일 미정");
        sunday.setWeekDay(0);
        monday.setWeekDay(1);
        saturday.setWeekDay(6);
        entityManager.flush();
        entityManager.clear();

        List<Study> studies = studyQueryRepository.searchAdminStudiesWithOffset(
                0,
                10,
                YEAR,
                SEMESTER,
                null,
                List.of(StudyStatus.APPROVED),
                List.of(new SortCriteria("week_day", SortDirection.ASC))
        );

        assertThat(studies).extracting(Study::getStudyName)
                .containsExactly("월요일", "토요일", "일요일", "요일 미정");
    }

    @Test
    void ordersMentorsAcrossTheWholeSemesterBeforeApplyingTheOffset() {
        User 다람Mentor = persistUser(900012L, "다람");
        User 가람Mentor = persistUser(900013L, "가람");
        User 나람Mentor = persistUser(900014L, "나람");
        persistApprovedStudy(다람Mentor, "다람 스터디");
        persistApprovedStudy(가람Mentor, "가람 스터디");
        persistApprovedStudy(나람Mentor, "나람 스터디");
        entityManager.flush();
        entityManager.clear();

        List<SortCriteria> sorting = List.of(new SortCriteria("primary_mentor_name", SortDirection.ASC));
        List<Study> firstPage = studyQueryRepository.searchAdminStudiesWithOffset(
                0, 2, YEAR, SEMESTER, null, List.of(StudyStatus.APPROVED), sorting);
        List<Study> secondPage = studyQueryRepository.searchAdminStudiesWithOffset(
                1, 2, YEAR, SEMESTER, null, List.of(StudyStatus.APPROVED), sorting);

        assertThat(firstPage).extracting(Study::getPrimaryMentorName)
                .containsExactly("가람", "나람");
        assertThat(secondPage).extracting(Study::getPrimaryMentorName)
                .containsExactly("다람");
    }

    @Test
    void ordersStatusDifficultyAndMenteeCountUsingTheirDomainValues() {
        User mentor = persistUser(900015L, "파생 컬럼 멘토");
        Study applicableEasy = persistApprovedStudy(mentor, "모집중 쉬움");
        Study closedHard = persistApprovedStudy(mentor, "마감 어려움");
        Study unspecified = persistApprovedStudy(mentor, "미정");
        applicableEasy.setRecruitStatus(RecruitStatus.APPLICABLE);
        applicableEasy.setDifficulty(StudyDifficulty.EASY);
        closedHard.setRecruitStatus(RecruitStatus.CLOSED);
        closedHard.setDifficulty(StudyDifficulty.HARD);
        closedHard.start();
        addTag(applicableEasy, "나");
        addTag(closedHard, "가");
        addMentee(applicableEasy, 900016L);
        addMentee(closedHard, 900017L);
        addMentee(closedHard, 900018L);
        entityManager.flush();
        entityManager.clear();

        List<StudyStatus> statuses = List.of(StudyStatus.APPROVED, StudyStatus.STARTED);
        assertThat(studyQueryRepository.searchAdminStudiesWithOffset(
                0, 10, YEAR, SEMESTER, null, statuses,
                List.of(new SortCriteria("recruit_status", SortDirection.ASC))))
                .extracting(Study::getStudyName)
                .containsExactly("모집중 쉬움", "마감 어려움", "미정");

        assertThat(studyQueryRepository.searchAdminStudiesWithOffset(
                0, 10, YEAR, SEMESTER, null, statuses,
                List.of(new SortCriteria("difficulty", SortDirection.ASC))))
                .extracting(Study::getStudyName)
                .containsExactly("모집중 쉬움", "마감 어려움", "미정");

        assertThat(studyQueryRepository.searchAdminStudiesWithOffset(
                0, 10, YEAR, SEMESTER, null, statuses,
                List.of(new SortCriteria("tags", SortDirection.ASC))))
                .extracting(Study::getStudyName)
                .containsExactly("마감 어려움", "모집중 쉬움", "미정");

        assertThat(studyQueryRepository.searchAdminStudiesWithOffset(
                0, 10, YEAR, SEMESTER, null, statuses,
                List.of(new SortCriteria("mentee_count", SortDirection.DESC))))
                .extracting(Study::getStudyName)
                .containsExactly("마감 어려움", "모집중 쉬움", "미정");

        assertThat(studyQueryRepository.searchAdminStudiesWithOffset(
                0, 10, YEAR, SEMESTER, null, statuses,
                List.of(new SortCriteria("study_status", SortDirection.ASC))))
                .last()
                .extracting(Study::getStudyName)
                .isEqualTo("마감 어려움");
    }

    private User persistUser(Long id, String name) {
        User user = User.createUser(id, name, id + "@forif.org", "010-0000-0000", "컴퓨터공학과");
        entityManager.persist(user);
        return user;
    }

    private Study persistApprovedStudy(User mentor, String name) {
        Study study = Study.createPendingStudy(mentor, YEAR, SEMESTER);
        study.setStudyName(name);
        study.approve();
        entityManager.persist(study);
        return study;
    }

    private void addMentee(Study study, Long userId) {
        User mentee = persistUser(userId, "멘티" + userId);
        entityManager.persist(StudyUser.create(study, mentee));
    }

    private void addTag(Study study, String name) {
        entityManager.createNativeQuery("INSERT INTO tb_study_tag (name) VALUES (?)")
                .setParameter(1, name)
                .executeUpdate();
        Number tagId = (Number) entityManager.createNativeQuery(
                        "SELECT tag_id FROM tb_study_tag WHERE name = ?")
                .setParameter(1, name)
                .getSingleResult();
        entityManager.createNativeQuery(
                        "INSERT INTO tb_study_tag_mapping (study_id, tag_id) VALUES (?, ?)")
                .setParameter(1, study.getId())
                .setParameter(2, tagId)
                .executeUpdate();
    }
}
