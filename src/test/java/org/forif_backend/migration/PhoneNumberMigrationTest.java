package org.forif_backend.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PhoneNumberMigrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.46");

    @Test
    @DisplayName("기존 형식 전화번호를 정규화하고 숫자가 없는 값은 NULL로 바꾼다")
    void normalizesExistingPhoneNumbersWithoutViolatingTheConstraint() {
        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .target("1")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()));
        insertUser(jdbcTemplate, 2022075741L, "missing@hanyang.ac.kr", "-");
        insertUser(jdbcTemplate, 20260001L, "formatted@hanyang.ac.kr", "010-1234-5678");
        insertUser(jdbcTemplate, 20260002L, "international@hanyang.ac.kr", "+82 10-9876-5432");

        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(phoneNumberOf(jdbcTemplate, 2022075741L)).isNull();
        assertThat(phoneNumberOf(jdbcTemplate, 20260001L)).isEqualTo("01012345678");
        assertThat(phoneNumberOf(jdbcTemplate, 20260002L)).isEqualTo("01098765432");
    }

    private void insertUser(JdbcTemplate jdbcTemplate, long userId, String email, String phoneNumber) {
        jdbcTemplate.update(
                """
                        INSERT INTO tb_user (created_at, updated_at, user_id, phone_num, email)
                        VALUES (CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), ?, ?, ?)
                        """,
                userId, phoneNumber, email
        );
    }

    private String phoneNumberOf(JdbcTemplate jdbcTemplate, long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT phone_num FROM tb_user WHERE user_id = ?", String.class, userId);
    }
}
