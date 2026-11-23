-- 합격자에게 배정한 부서.
-- 지원자의 1지망·2지망 중 회장단이 고른 모집 부서를 담고, 합격이 아니면 비운다.
-- 합격자를 차기 운영진 명단(tb_forif_team)에 올릴 때 이 부서로 등록한다.
ALTER TABLE tb_staff_interview_record
    ADD COLUMN passed_department_id INT NULL,
    ADD CONSTRAINT fk_staff_interview_record_passed_department
        FOREIGN KEY (passed_department_id)
        REFERENCES tb_staff_recruitment_department (recruitment_department_id);
