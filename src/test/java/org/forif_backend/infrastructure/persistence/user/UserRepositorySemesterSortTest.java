package org.forif_backend.infrastructure.persistence.user;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.user.User;
import org.forif_backend.infrastructure.persistence.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class, UserRepositoryImpl.class})
class UserRepositorySemesterSortTest {

    private static final int YEAR = 2026;
    private static final int SEMESTER = 1;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepositoryImpl userRepository;

    @Test
    void ordersOnlySelectedSemesterMembersBeforeApplyingTheOffset() {
        User 다람 = persistUser(910001L, "다람");
        User 가람 = persistUser(910002L, "가람");
        User 나람 = persistUser(910003L, "나람");
        User 이전학기부원 = persistUser(910004L, "가장먼저");
        다람.updateProfile("A학과", null);
        가람.updateProfile("B학과", null);
        나람.updateProfile("A학과", null);
        addMember(다람, YEAR, SEMESTER, "다람 스터디");
        addMember(가람, YEAR, SEMESTER, "가람 스터디");
        addMember(나람, YEAR, SEMESTER, "나람 스터디");
        addMember(이전학기부원, 2025, 2, "이전 학기 스터디");
        entityManager.flush();
        entityManager.clear();

        List<SortCriteria> sorting = List.of(new SortCriteria("userName", SortDirection.ASC));
        List<User> firstPage = userRepository.searchUsersByYearSemesterWithOffset(
                YEAR, SEMESTER, 0, 2, null, sorting);
        List<User> secondPage = userRepository.searchUsersByYearSemesterWithOffset(
                YEAR, SEMESTER, 1, 2, null, sorting);

        assertThat(firstPage).extracting(User::getUserName).containsExactly("가람", "나람");
        assertThat(secondPage).extracting(User::getUserName).containsExactly("다람");

        assertThat(userRepository.searchUsersByYearSemesterWithOffset(
                YEAR, SEMESTER, 0, 10, null,
                List.of(new SortCriteria("department", SortDirection.ASC))))
                .extracting(User::getUserName)
                .containsExactly("나람", "다람", "가람");
        assertThat(userRepository.searchUsersByYearSemesterWithOffset(
                YEAR, SEMESTER, 0, 10, null,
                List.of(new SortCriteria("userId", SortDirection.ASC))))
                .extracting(User::getUserName)
                .containsExactly("다람", "가람", "나람");
    }

    private User persistUser(Long id, String name) {
        User user = User.createUser(id, name, id + "@forif.org", "010-0000-0000", "컴퓨터공학과");
        entityManager.persist(user);
        return user;
    }

    private void addMember(User member, int year, int semester, String studyName) {
        User mentor = persistUser(member.getId() + 100_000L, "멘토" + member.getId());
        Study study = Study.createPendingStudy(mentor, year, semester);
        study.setStudyName(studyName);
        study.approve();
        entityManager.persist(study);
        entityManager.persist(StudyUser.create(study, member));
    }
}
