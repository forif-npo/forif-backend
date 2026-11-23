-- 운영진 모집 면접 진행 기록.
-- 면접자별 진행 상태, 공통·부서 질문에 대한 답변, 면접자마다 따로 묻는 기타 질문을 담는다.

-- 기록이 없는 지원자는 면접 전이다. 상태를 처음 바꿀 때 만든다.
CREATE TABLE tb_staff_interview_record (
    interview_record_id INT NOT NULL AUTO_INCREMENT,
    application_id INT NOT NULL,
    -- BEFORE(면접 전) / DONE(면접 완료) / PASSED(합격) / FAILED(불합격)
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (interview_record_id),
    UNIQUE KEY uk_staff_interview_record_application (application_id),
    CONSTRAINT fk_staff_interview_record_application
        FOREIGN KEY (application_id) REFERENCES tb_staff_application (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 공통·부서 질문에 대한 면접자의 답변. 질문마다 하나다.
CREATE TABLE tb_staff_interview_answer (
    interview_answer_id INT NOT NULL AUTO_INCREMENT,
    application_id INT NOT NULL,
    interview_question_id INT NOT NULL,
    answer TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (interview_answer_id),
    UNIQUE KEY uk_staff_interview_answer (application_id, interview_question_id),
    KEY idx_staff_interview_answer_question (interview_question_id),
    CONSTRAINT fk_staff_interview_answer_application
        FOREIGN KEY (application_id) REFERENCES tb_staff_application (application_id),
    CONSTRAINT fk_staff_interview_answer_question
        FOREIGN KEY (interview_question_id) REFERENCES tb_staff_interview_question (interview_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 면접자 한 명에게만 묻는 기타 질문과 답변.
CREATE TABLE tb_staff_interview_extra_question (
    interview_extra_question_id INT NOT NULL AUTO_INCREMENT,
    application_id INT NOT NULL,
    content VARCHAR(500) NOT NULL,
    answer TEXT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (interview_extra_question_id),
    KEY idx_staff_interview_extra_question_order (application_id, display_order),
    CONSTRAINT fk_staff_interview_extra_question_application
        FOREIGN KEY (application_id) REFERENCES tb_staff_application (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
