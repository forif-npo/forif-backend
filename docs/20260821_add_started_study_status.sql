-- 배포 전에 운영 DB에서 한 번 실행한다.
-- Hibernate 운영 프로필은 ddl-auto=validate이므로 native ENUM의 허용값을 자동으로 늘리지 않는다.
-- 실행 전 현재 컬럼 속성을 확인한다.
-- SHOW COLUMNS FROM tb_study LIKE 'study_status';

ALTER TABLE tb_study
    MODIFY COLUMN study_status ENUM(
        'PENDING',
        'APPROVED',
        'STARTED',
        'REJECTED',
        'RE_APPLIED'
    ) NOT NULL;
