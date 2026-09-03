package org.forif_backend.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * db/migration 의 마이그레이션이 실제 MySQL에서 실행되는지, 그리고 그 결과 스키마가
 * JPA 엔티티와 일치하는지 검증한다.
 *
 * <p>운영(release)은 Flyway로 스키마를 적용한 뒤 ddl-auto: validate 로 엔티티와의 일치를
 * 재확인한다. 그런데 이 경로를 배포 전에 실행해보는 환경이 따로 없어서, 마이그레이션의
 * 문법 오류나 누락은 운영 컨테이너 부팅 시점에야 드러난다. MySQL은 DDL이 트랜잭션이
 * 아니라 중간에 실패하면 스키마가 반쯤 바뀐 채로 남고 이후 재부팅이 계속 막힌다.
 *
 * <p>이 테스트가 그 유일한 사전 검증 지점이다. 나머지 테스트는 H2 + create-drop 이라
 * MySQL 전용 문법을 검증하지 못한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers
class FlywayMigrationSchemaTest {

    /** 운영 서버와 같은 MySQL 버전을 쓴다. */
    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.46");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("빈 DB에 마이그레이션을 적용하면 엔티티와 일치하는 스키마가 만들어진다")
    void migrationsProduceSchemaMatchingEntities() {
        // 컨텍스트 로딩 자체가 검증이다.
        // Flyway가 V1부터 순서대로 적용하고, 이어서 Hibernate가 validate로 엔티티와 대조한다.
        // 둘 중 하나라도 어긋나면 이 테스트는 컨텍스트 로딩 단계에서 실패한다.
    }

    @Test
    @DisplayName("전화번호는 숫자만 포함해야 하며 중복될 수 없다")
    void phoneNumberConstraintsAreEnforced() {
        insertUser(20260001L, "first@hanyang.ac.kr", "01012345678");

        assertThatThrownBy(() -> insertUser(20260002L, "duplicate@hanyang.ac.kr", "01012345678"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertUser(20260003L, "formatted@hanyang.ac.kr", "010-1234-5678"))
                .isInstanceOf(DataIntegrityViolationException.class);
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
