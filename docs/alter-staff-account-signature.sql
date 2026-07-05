-- 수료증 서명 이미지 컬럼 추가
-- 로컬/dev는 ddl-auto: update로 자동 반영되지만, release는 validate이므로 배포 전 실행 필요.

ALTER TABLE tb_staff_account
    ADD COLUMN signature_object_key VARCHAR(300) NULL;
