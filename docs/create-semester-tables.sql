-- 활동 학기 관리 테이블 (FOR-108)
--
-- 로컬(local 프로파일)만 ddl-auto: update라 자동 생성된다.
-- dev 서버와 운영 서버는 release 프로파일 = ddl-auto: validate 이므로
-- JAR을 교체하기 "전에" 이 스크립트를 먼저 돌려야 한다.
-- 순서를 바꾸면 검증 실패로 기동에 실패하고 컨테이너가 재시작을 반복한다.

CREATE TABLE IF NOT EXISTS tb_active_semester (
    active_semester_id INT         NOT NULL,
    act_year           INT         NOT NULL,
    act_semester       INT         NOT NULL,
    updated_by         BIGINT      NULL,
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (active_semester_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS tb_semester_change_log (
    log_id        BIGINT      NOT NULL AUTO_INCREMENT,
    from_year     INT         NOT NULL,
    from_semester INT         NOT NULL,
    to_year       INT         NOT NULL,
    to_semester   INT         NOT NULL,
    changed_by    BIGINT      NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (log_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 초기값: 배포 직후 동작이 바뀌지 않도록 현재 운영 중인 학기를 넣는다.
-- 값이 없으면 서비스가 날짜 기준으로 폴백하지만, 명시 설정이 원칙이다.
INSERT INTO tb_active_semester (active_semester_id, act_year, act_semester, updated_by, created_at, updated_at)
VALUES (1, 2026, 1, NULL, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE active_semester_id = active_semester_id;
