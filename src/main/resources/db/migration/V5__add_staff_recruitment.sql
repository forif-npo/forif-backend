-- 운영진 모집 회차와 하위 부서·문항.
-- 스터디 지원(tb_user_apply)과는 별도 모델이며 기존 테이블을 변경하지 않는다.

CREATE TABLE tb_staff_recruitment (
    recruitment_id INT NOT NULL AUTO_INCREMENT,
    act_year INT NOT NULL,
    act_semester INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    summary VARCHAR(200) NULL,
    description TEXT NULL,
    starts_at DATETIME(6) NULL,
    ends_at DATETIME(6) NULL,
    status VARCHAR(20) NOT NULL,
    -- 게시 중일 때만 1, 그 외에는 NULL. 유니크 제약으로 동시 게시 회차를 하나로 제한한다.
    published_slot INT NULL,
    ended_at DATETIME(6) NULL,
    close_reason VARCHAR(200) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (recruitment_id),
    UNIQUE KEY uk_staff_recruitment_published_slot (published_slot),
    KEY idx_staff_recruitment_status_period (status, starts_at, ends_at),
    CONSTRAINT ck_staff_recruitment_period
        CHECK (starts_at IS NULL OR ends_at IS NULL OR starts_at < ends_at),
    CONSTRAINT ck_staff_recruitment_semester
        CHECK (act_semester IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_staff_recruitment_department (
    recruitment_department_id INT NOT NULL AUTO_INCREMENT,
    recruitment_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200) NULL,
    display_order INT NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (recruitment_department_id),
    UNIQUE KEY uk_staff_recruitment_department_name (recruitment_id, name),
    KEY idx_staff_recruitment_department_order (recruitment_id, display_order),
    CONSTRAINT fk_staff_recruitment_department_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES tb_staff_recruitment (recruitment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_staff_recruitment_question (
    recruitment_question_id INT NOT NULL AUTO_INCREMENT,
    recruitment_id INT NOT NULL,
    label VARCHAR(100) NOT NULL,
    description VARCHAR(200) NULL,
    type VARCHAR(20) NOT NULL,
    required TINYINT(1) NOT NULL DEFAULT 0,
    max_length INT NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (recruitment_question_id),
    KEY idx_staff_recruitment_question_order (recruitment_id, display_order),
    CONSTRAINT fk_staff_recruitment_question_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES tb_staff_recruitment (recruitment_id),
    CONSTRAINT ck_staff_recruitment_question_max_length
        CHECK (max_length BETWEEN 1 AND 5000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
