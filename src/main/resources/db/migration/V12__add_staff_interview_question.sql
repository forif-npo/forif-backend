-- 운영진 모집 회차의 면접 질문.
-- 회장단이 공통 질문과 부서별 질문을 미리 써 두면 모든 면접자 화면에 똑같이 나온다.
-- 부서 질문은 지원자의 1지망·2지망 부서에 해당하는 것만 그 지원자에게 보인다.

CREATE TABLE tb_staff_interview_question (
    interview_question_id INT NOT NULL AUTO_INCREMENT,
    recruitment_id INT NOT NULL,
    -- NULL이면 모든 지원자에게 묻는 공통 질문이다.
    recruitment_department_id INT NULL,
    content VARCHAR(500) NOT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (interview_question_id),
    KEY idx_staff_interview_question_order (recruitment_id, recruitment_department_id, display_order),
    CONSTRAINT fk_staff_interview_question_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES tb_staff_recruitment (recruitment_id),
    CONSTRAINT fk_staff_interview_question_department
        FOREIGN KEY (recruitment_department_id)
        REFERENCES tb_staff_recruitment_department (recruitment_department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
