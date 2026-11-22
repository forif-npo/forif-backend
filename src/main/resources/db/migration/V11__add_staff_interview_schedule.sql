-- 운영진 모집 회차의 면접 일정 조율.
-- 회장단이 면접을 볼 수 있는 시간과, 지원자별로 확정한 면접 시각을 담는다.
-- 기존 테이블은 면접 일정(tb_staff_interview_plan)에 확정 시각 컬럼만 더한다.

-- 회장단이 함께 면접을 보므로 회차마다 하나의 공용 시간표로 둔다.
-- 지원자 선택과 같이 날짜와 시작 분을 값으로 담는다.
CREATE TABLE tb_staff_interview_host_slot (
    interview_host_slot_id INT NOT NULL AUTO_INCREMENT,
    recruitment_id INT NOT NULL,
    slot_date DATE NOT NULL,
    start_minute INT NOT NULL,
    PRIMARY KEY (interview_host_slot_id),
    UNIQUE KEY uk_staff_interview_host_slot (recruitment_id, slot_date, start_minute),
    CONSTRAINT fk_staff_interview_host_slot_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES tb_staff_recruitment (recruitment_id),
    CONSTRAINT ck_staff_interview_host_slot_minute
        CHECK (start_minute >= 0 AND start_minute < 1440)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 지원자별 면접 시각. 면접은 한 타임에 한 명이라 회차 안에서 칸마다 한 명,
-- 지원자마다 한 칸이다. 두 유니크 제약이 동시 배정도 막는다.
-- 면접 길이는 면접 일정의 칸 길이를 따르므로 시작 시각만 담는다.
CREATE TABLE tb_staff_interview_assignment (
    interview_assignment_id INT NOT NULL AUTO_INCREMENT,
    recruitment_id INT NOT NULL,
    application_id INT NOT NULL,
    slot_date DATE NOT NULL,
    start_minute INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (interview_assignment_id),
    UNIQUE KEY uk_staff_interview_assignment_application (application_id),
    UNIQUE KEY uk_staff_interview_assignment_slot (recruitment_id, slot_date, start_minute),
    CONSTRAINT fk_staff_interview_assignment_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES tb_staff_recruitment (recruitment_id),
    CONSTRAINT fk_staff_interview_assignment_application
        FOREIGN KEY (application_id) REFERENCES tb_staff_application (application_id),
    CONSTRAINT ck_staff_interview_assignment_minute
        CHECK (start_minute >= 0 AND start_minute < 1440)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 회장단이 일정을 확정한 시각. 확정 뒤에도 면접이 길어지면 시각을 옮길 수 있다.
ALTER TABLE tb_staff_interview_plan
    ADD COLUMN schedule_confirmed_at DATETIME(6) NULL;
