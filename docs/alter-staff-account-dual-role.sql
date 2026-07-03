-- 스태프 계정 1인 다역할(MENTOR + ADMIN) 지원 마이그레이션
-- 기존: PK = user_id (1인 1계정)
-- 변경: PK = staff_account_id (서로게이트), (user_id, role) 유니크
--
-- 적용 대상: 로컬 / dev / prod MySQL 모두 배포 전 1회 실행 필요.
-- 백엔드 엔티티(StaffAccount)의 서로게이트 키 변경과 함께 배포해야 한다.

ALTER TABLE tb_staff_account
    DROP PRIMARY KEY,
    ADD COLUMN staff_account_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST,
    ADD CONSTRAINT uq_staff_account_user_role UNIQUE (user_id, role);
