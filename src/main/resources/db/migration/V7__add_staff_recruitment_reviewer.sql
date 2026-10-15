-- 모집 회차별 지원서 열람 권한(reviewer).
-- 전역 ADMIN 권한을 넓히지 않고 회차 단위로만 열람을 허용하기 위한 테이블이다.
-- 회수·만료 이력을 남겨야 하므로 행을 지우지 않고 revoked_at으로 표시한다.

CREATE TABLE tb_staff_recruitment_reviewer (
    recruitment_reviewer_id INT NOT NULL AUTO_INCREMENT,
    recruitment_id INT NOT NULL,
    user_id BIGINT NOT NULL,
    granted_by BIGINT NOT NULL,
    granted_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    revoked_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (recruitment_reviewer_id),
    -- 같은 회차에 같은 사람을 두 번 등록하지 않는다. 재부여는 기존 행을 되살린다.
    UNIQUE KEY uk_staff_recruitment_reviewer (recruitment_id, user_id),
    KEY idx_staff_recruitment_reviewer_active (recruitment_id, revoked_at, expires_at),
    KEY idx_staff_recruitment_reviewer_user (user_id, revoked_at, expires_at),
    CONSTRAINT fk_staff_recruitment_reviewer_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES tb_staff_recruitment (recruitment_id),
    CONSTRAINT fk_staff_recruitment_reviewer_user
        FOREIGN KEY (user_id) REFERENCES tb_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
