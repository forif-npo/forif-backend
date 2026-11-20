package org.forif_backend.infrastructure.persistence.team;

import jakarta.persistence.EntityManager;
import org.forif_backend.common.config.JpaAuditingConfig;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentDepartmentSpec;
import org.forif_backend.domain.team.ClubDepartment;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class ClubDepartmentRelationJpaTest {

    @Autowired private EntityManager entityManager;

    @Test
    void 운영진_이력과_모집_부서가_같은_공통_부서_ID를_저장한다() {
        ClubDepartment department = ClubDepartment.create("SW");
        entityManager.persist(department);

        User user = User.createUser(20260001L, "테스트 운영진", "operator@example.com", null, "예시 학과");
        entityManager.persist(user);
        ForifTeam team = ForifTeam.create(user, 2026, 2, "SW");
        team.assignClubDepartment(department);
        entityManager.persist(team);

        StaffRecruitment recruitment = StaffRecruitment.create(2027, 1, "2027-1", null, "안내", null, null);
        recruitment.mergeDepartments(List.of(
                new StaffRecruitmentDepartmentSpec(null, "SW", null, 1, true)), false);
        recruitment.getDepartments().get(0).assignClubDepartment(department);
        entityManager.persist(recruitment);
        entityManager.flush();
        entityManager.clear();

        Long teamDepartmentId = ((Number) entityManager.createNativeQuery(
                "SELECT club_department_id FROM tb_forif_team WHERE forif_team_id = :id")
                .setParameter("id", team.getId()).getSingleResult()).longValue();
        Long recruitmentDepartmentId = ((Number) entityManager.createNativeQuery(
                "SELECT club_department_id FROM tb_staff_recruitment_department WHERE recruitment_department_id = :id")
                .setParameter("id", recruitment.getDepartments().get(0).getId()).getSingleResult()).longValue();

        assertThat(teamDepartmentId).isEqualTo(department.getId());
        assertThat(recruitmentDepartmentId).isEqualTo(department.getId());
    }
}
