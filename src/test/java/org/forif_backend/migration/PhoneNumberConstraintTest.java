package org.forif_backend.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers
class PhoneNumberConstraintTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.46");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("전화번호는 숫자만 포함해야 하며 중복될 수 없다")
    void phoneNumberConstraintsAreEnforced() {
        insertUser(20260001L, "first@hanyang.ac.kr", "01012345678");

        assertThatThrownBy(() -> insertUser(20260002L, "duplicate@hanyang.ac.kr", "01012345678"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertUser(20260003L, "formatted@hanyang.ac.kr", "010-1234-5678"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertUser(20260006L, "empty@hanyang.ac.kr", ""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("전화번호가 없으면 NULL을 허용한다")
    void allowsNullPhoneNumbers() {
        assertThatCode(() -> insertUser(20260004L, "missing-first@hanyang.ac.kr", null))
                .doesNotThrowAnyException();
        assertThatCode(() -> insertUser(20260005L, "missing-second@hanyang.ac.kr", null))
                .doesNotThrowAnyException();
    }

    private void insertUser(long userId, String email, String phoneNumber) {
        jdbcTemplate.update(
                """
                        INSERT INTO tb_user (created_at, updated_at, user_id, phone_num, email)
                        VALUES (CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), ?, ?, ?)
                        """,
                userId, phoneNumber, email
        );
    }
}
