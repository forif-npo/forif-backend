-- 자율스터디 판정 및 학기당 1개 제약
-- release 프로필은 ddl-auto: validate 이므로 애플리케이션 배포 전에 실행해야 합니다.
-- MySQL DDL은 암묵적으로 커밋되므로, 아래 순서를 한 트랜잭션으로 감싸지 않습니다.

ALTER TABLE tb_study
    ADD COLUMN autonomous_flag TINYINT NULL DEFAULT NULL;

-- [사전 확인]
-- 기존에 '자율스터디'라는 이름의 데이터가 있으면 실제 자율스터디인지 먼저 확인합니다.
-- 이름만으로 일반 스터디와 구분할 수 없으므로 자동 UPDATE를 실행하지 않습니다.
SELECT study_id, act_year, act_semester, study_name, study_status
FROM tb_study
WHERE study_name = '자율스터디'
ORDER BY act_year, act_semester, study_id;

-- [기존 자율스터디 이관]
-- 위 조회 결과에서 실제 자율스터디로 확인한 study_id만 명시해 실행합니다.
-- 기존 데이터가 없다면 이 단계는 건너뜁니다.
-- UPDATE tb_study
-- SET autonomous_flag = 1
-- WHERE study_id IN (<확인한_자율스터디_ID>);

-- [유니크 제약 추가 전 확인]
-- 결과가 있으면 동일 학기에 자율스터디가 둘 이상이므로 데이터를 정리한 뒤 다음 DDL을 실행합니다.
SELECT act_year, act_semester, COUNT(*) AS autonomous_study_count
FROM tb_study
WHERE autonomous_flag = 1
GROUP BY act_year, act_semester
HAVING COUNT(*) > 1;

ALTER TABLE tb_study
    ADD CONSTRAINT uk_study_autonomous_semester
        UNIQUE (act_year, act_semester, autonomous_flag);
