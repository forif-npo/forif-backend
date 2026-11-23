package org.forif_backend.infrastructure.persistence.staffrecruitment;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.staffrecruitment.StaffApplication;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, StaffApplicationRepositoryImpl.class})
class StaffApplicationRepositorySortTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2027, 1, 10, 12, 0);

    @Autowired private EntityManager entityManager;
    @Autowired private StaffApplicationRepositoryImpl repository;

    private Integer recruitmentId;

    @BeforeEach
    void setUp() {
        StaffRecruitment recruitment = StaffRecruitment.create(2027, 1, "2027-1", null, "모집 안내",
                BASE.minusDays(5), BASE.plusDays(5));
        entityManager.persist(recruitment);
        recruitment.publish();
        // 제출 순서와 학과 순서를 일부러 엇갈리게 둔다.
        persist(recruitment, 1L, "나 학과", BASE.minusHours(1));
        persist(recruitment, 2L, "가 학과", BASE.minusHours(3));
        persist(recruitment, 3L, "다 학과", BASE.minusHours(2));
        entityManager.flush();
        entityManager.clear();
        recruitmentId = recruitment.getId();
    }

    @Test
    void 정렬이_없으면_최신_제출부터_온다() {
        assertThat(departments(List.of())).containsExactly("다 학과", "가 학과", "나 학과");
    }

    @Test
    void 학과로_정렬한다() {
        assertThat(departments(List.of(new SortCriteria("department", SortDirection.ASC))))
                .containsExactly("가 학과", "나 학과", "다 학과");
        assertThat(departments(List.of(new SortCriteria("department", SortDirection.DESC))))
                .containsExactly("다 학과", "나 학과", "가 학과");
    }

    @Test
    void 제출_시각으로_정렬한다() {
        assertThat(departments(List.of(new SortCriteria("submittedAt", SortDirection.ASC))))
                .containsExactly("가 학과", "다 학과", "나 학과");
        assertThat(departments(List.of(new SortCriteria("submittedAt", SortDirection.DESC))))
                .containsExactly("나 학과", "다 학과", "가 학과");
    }

    @Test
    void 정렬은_페이지를_넘어서도_유지된다() {
        List<SortCriteria> byDepartment = List.of(new SortCriteria("department", SortDirection.ASC));
        assertThat(repository.findForAdminWithOffset(recruitmentId, null, null, 1, 2, byDepartment))
                .extracting(StaffApplication::getApplicantDepartment)
                .containsExactly("다 학과");
    }

    private List<String> departments(List<SortCriteria> sorting) {
        return repository.findForAdminWithOffset(recruitmentId, null, null, 0, 10, sorting).stream()
                .map(StaffApplication::getApplicantDepartment)
                .toList();
    }

    private void persist(StaffRecruitment recruitment, Long userId, String department, LocalDateTime submittedAt) {
        User user = User.createUser(userId, "테스트 회원" + userId, userId + "@example.com",
                "0100000000" + userId, department);
        entityManager.persist(user);
        entityManager.persist(StaffApplication.submit(recruitment, user, user.getUserName(), user.getEmail(),
                user.getPhoneNum(), department, submittedAt));
    }
}
