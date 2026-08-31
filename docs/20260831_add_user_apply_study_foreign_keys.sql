-- MySQL 8 / InnoDB용 수동 운영 마이그레이션
-- 이 프로젝트는 Flyway/Liquibase를 사용하지 않으므로, 배포 전에 운영 DB에서 한 번 실행한다.
-- 아래 고아 데이터 조회 결과가 0건인 것을 확인한 뒤 ALTER TABLE을 실행한다.

SELECT
    ua.user_apply_id,
    ua.primary_study,
    ua.secondary_study
FROM tb_user_apply ua
LEFT JOIN tb_study primary_study ON primary_study.study_id = ua.primary_study
LEFT JOIN tb_study secondary_study ON secondary_study.study_id = ua.secondary_study
WHERE primary_study.study_id IS NULL
   OR (ua.secondary_study IS NOT NULL AND secondary_study.study_id IS NULL);

-- 신청 이력이 존재하는 스터디는 SQL 직접 삭제도 불가능하게 한다.
-- RESTRICT는 자식(tb_user_apply) 행이 있을 때 부모(tb_study) 행 삭제를 거부한다.
ALTER TABLE tb_user_apply
    ADD CONSTRAINT fk_user_apply_primary_study
        FOREIGN KEY (primary_study)
        REFERENCES tb_study (study_id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_user_apply_secondary_study
        FOREIGN KEY (secondary_study)
        REFERENCES tb_study (study_id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT;
