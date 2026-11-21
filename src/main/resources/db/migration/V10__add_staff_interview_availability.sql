-- 운영진 모집 회차의 면접 일정 후보와, 지원자가 고른 면접 가능 시간.
-- 지원 폼 안에서 함께 받지만 일반 문항(tb_staff_recruitment_question)과 달리
-- 시간표 형태로 수집·집계해야 하므로 별도 구조로 둔다.
-- 기존 테이블은 변경하지 않는다.

CREATE TABLE tb_staff_interview_plan (
    interview_plan_id INT NOT NULL AUTO_INCREMENT,
    recruitment_id INT NOT NULL,
    -- 수집 시간 범위. start_hour는 포함, end_hour는 제외다.
    start_hour INT NOT NULL,
    end_hour INT NOT NULL,
    -- 한 칸의 길이(분). 15·30·60처럼 회차마다 정한다.
    -- 범위를 나누어떨어지게 해야 하므로 저장 시 검증한다.
    slot_minutes INT NOT NULL DEFAULT 30,
    -- 지원서 제출에 최소 한 칸을 요구할지 여부
    required TINYINT(1) NOT NULL DEFAULT 1,
    -- 지원 화면에 띄울 안내 문구
    guide VARCHAR(200) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (interview_plan_id),
    -- 회차마다 면접 일정은 하나다.
    UNIQUE KEY uk_staff_interview_plan_recruitment (recruitment_id),
    CONSTRAINT fk_staff_interview_plan_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES tb_staff_recruitment (recruitment_id),
    CONSTRAINT ck_staff_interview_plan_hours
        CHECK (start_hour >= 0 AND end_hour <= 24 AND start_hour < end_hour),
    CONSTRAINT ck_staff_interview_plan_slot_minutes
        CHECK (slot_minutes IN (10, 15, 20, 30, 60))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_staff_interview_plan_date (
    interview_plan_date_id INT NOT NULL AUTO_INCREMENT,
    interview_plan_id INT NOT NULL,
    interview_date DATE NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (interview_plan_date_id),
    UNIQUE KEY uk_staff_interview_plan_date (interview_plan_id, interview_date),
    KEY idx_staff_interview_plan_date_order (interview_plan_id, display_order),
    CONSTRAINT fk_staff_interview_plan_date_plan
        FOREIGN KEY (interview_plan_id) REFERENCES tb_staff_interview_plan (interview_plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_staff_application_interview_slot (
    application_interview_slot_id INT NOT NULL AUTO_INCREMENT,
    application_id INT NOT NULL,
    -- 후보 날짜 행을 참조하지 않고 값으로 보관한다.
    -- 제출 뒤 회장단이 후보 날짜를 바꿔도 지원자가 고른 내용이 사라지지 않아야 한다.
    slot_date DATE NOT NULL,
    -- 칸의 시작 시각을 자정부터의 분으로 보관한다.
    -- 칸 길이가 15분이든 60분이든 같은 방식으로 담긴다. 09:30이면 570이다.
    start_minute INT NOT NULL,
    PRIMARY KEY (application_interview_slot_id),
    UNIQUE KEY uk_staff_application_interview_slot (application_id, slot_date, start_minute),
    KEY idx_staff_application_interview_slot_time (slot_date, start_minute),
    CONSTRAINT fk_staff_application_interview_slot_application
        FOREIGN KEY (application_id) REFERENCES tb_staff_application (application_id),
    CONSTRAINT ck_staff_application_interview_slot_minute
        CHECK (start_minute >= 0 AND start_minute < 1440)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
