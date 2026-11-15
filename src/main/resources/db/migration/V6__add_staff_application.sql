-- 운영진 지원서와 지망·답변.
-- 스터디 지원(tb_user_apply)과 별도 모델이며 기존 테이블을 변경하지 않는다.
-- 제출 당시 개인정보 snapshot은 계약(r2)에 따라 영구 보관하며 자동 삭제 작업을 두지 않는다.

CREATE TABLE tb_staff_application (
    application_id INT NOT NULL AUTO_INCREMENT,
    recruitment_id INT NOT NULL,
    -- 회원 탈퇴 시 연결만 끊고 제출 snapshot은 남긴다.
    applicant_user_id BIGINT NULL,
    applicant_name VARCHAR(50) NOT NULL,
    applicant_email VARCHAR(100) NOT NULL,
    applicant_phone_num VARCHAR(20) NULL,
    applicant_department VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL,
    submitted_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (application_id),
    -- 회차당 회원 1명은 한 번만 제출한다. 동시 요청도 이 제약으로 막는다.
    UNIQUE KEY uk_staff_application_recruitment_user (recruitment_id, applicant_user_id),
    KEY idx_staff_application_list (recruitment_id, submitted_at, application_id),
    CONSTRAINT fk_staff_application_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES tb_staff_recruitment (recruitment_id),
    CONSTRAINT fk_staff_application_user
        FOREIGN KEY (applicant_user_id) REFERENCES tb_user (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_staff_application_choice (
    application_choice_id INT NOT NULL AUTO_INCREMENT,
    application_id INT NOT NULL,
    recruitment_department_id INT NOT NULL,
    priority INT NOT NULL,
    PRIMARY KEY (application_choice_id),
    UNIQUE KEY uk_staff_application_choice_priority (application_id, priority),
    UNIQUE KEY uk_staff_application_choice_department (application_id, recruitment_department_id),
    KEY idx_staff_application_choice_department (recruitment_department_id),
    CONSTRAINT fk_staff_application_choice_application
        FOREIGN KEY (application_id) REFERENCES tb_staff_application (application_id),
    CONSTRAINT fk_staff_application_choice_department
        FOREIGN KEY (recruitment_department_id)
        REFERENCES tb_staff_recruitment_department (recruitment_department_id),
    CONSTRAINT ck_staff_application_choice_priority CHECK (priority IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_staff_application_answer (
    application_answer_id INT NOT NULL AUTO_INCREMENT,
    application_id INT NOT NULL,
    recruitment_question_id INT NOT NULL,
    -- 문항이 나중에 바뀌어도 제출 당시 의미가 유지되도록 label과 type을 함께 보관한다.
    question_label VARCHAR(100) NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    answer TEXT NULL,
    PRIMARY KEY (application_answer_id),
    UNIQUE KEY uk_staff_application_answer_question (application_id, recruitment_question_id),
    KEY idx_staff_application_answer_question (recruitment_question_id),
    CONSTRAINT fk_staff_application_answer_application
        FOREIGN KEY (application_id) REFERENCES tb_staff_application (application_id),
    CONSTRAINT fk_staff_application_answer_question
        FOREIGN KEY (recruitment_question_id)
        REFERENCES tb_staff_recruitment_question (recruitment_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
