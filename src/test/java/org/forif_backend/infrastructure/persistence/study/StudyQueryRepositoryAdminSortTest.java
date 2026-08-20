package org.forif_backend.infrastructure.persistence.study;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyStatus;
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
}
