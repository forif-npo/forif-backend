package org.forif_backend.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ClubDepartmentMigrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.46");

    @Test
    void 기존_운영진_이력만_연결하고_이전_모집_데이터는_보존한다() {
        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .target("8")
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()));
        jdbc.update("""
                INSERT INTO tb_user (created_at, updated_at, user_id, email)
                VALUES
                    (CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 20260001, 'operator1@example.com'),
                    (CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 20260002, 'operator2@example.com'),
                    (CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 20260003, 'operator3@example.com')
                """);
        jdbc.update("""
                INSERT INTO tb_forif_team
                    (act_year, act_semester, user_id, club_department, created_at, updated_at)
                VALUES
                    (2026, 2, 20260001, 'SW', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
                    (2026, 2, 20260002, 'sw', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
                    (2026, 2, 20260003, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """);
        jdbc.update("""
                INSERT INTO tb_staff_recruitment
                    (act_year, act_semester, title, status, version, created_at, updated_at)
                VALUES (2027, 1, '2027-1', 'DRAFT', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """);
        jdbc.update("""
                INSERT INTO tb_staff_recruitment_department
                    (recruitment_id, name, display_order, active)
                VALUES (1, '기획팀', 1, 1), (1, 'sw팀', 2, 1), (1, '회장', 3, 1)
                """);

        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        Long operatorDepartmentId = jdbc.queryForObject(
                "SELECT club_department_id FROM tb_forif_team WHERE user_id = 20260001", Long.class);
        assertThat(operatorDepartmentId).isNotNull();
        assertThat(jdbc.queryForObject(
                "SELECT club_department_id FROM tb_forif_team WHERE user_id = 20260002", Long.class))
                .isEqualTo(operatorDepartmentId);
        assertThat(jdbc.queryForObject(
                "SELECT club_department_id FROM tb_forif_team WHERE user_id = 20260003", Long.class))
                .isNull();
        assertThat(jdbc.queryForList(
                "SELECT club_department FROM tb_forif_team ORDER BY user_id", String.class))
                .containsExactly("SW", "sw", null);
        assertThat(jdbc.queryForObject(
                "SELECT name FROM tb_club_department WHERE club_department_id = ?", String.class,
                operatorDepartmentId)).isEqualTo("SW");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM tb_club_department", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForList(
                "SELECT name FROM tb_staff_recruitment_department ORDER BY display_order", String.class))
                .containsExactly("기획팀", "sw팀", "회장");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM tb_staff_recruitment_department WHERE club_department_id IS NULL",
                Integer.class)).isEqualTo(3);
    }
}
