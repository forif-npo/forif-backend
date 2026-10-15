-- 명예의 전당 전시 콘텐츠.
-- 관리자가 수동으로 등록·게시하며 자동 점수·랭킹은 이번 범위가 아니다.
-- 공개 승인을 받은 소개는 회원 탈퇴와 별개로 남기므로 회원 FK는 ON DELETE SET NULL이다.

CREATE TABLE tb_hall_of_fame_entry (
    entry_id INT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    -- 회원 정보가 바뀌거나 탈퇴해도 게시된 소개가 유지되도록 이름을 복사해 둔다.
    member_name VARCHAR(50) NOT NULL,
    act_year INT NOT NULL,
    act_semester INT NOT NULL,
    slug VARCHAR(80) NOT NULL,
    headline VARCHAR(200) NULL,
    introduction TEXT NULL,
    image_object_key VARCHAR(300) NULL,
    status VARCHAR(20) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    published_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (entry_id),
    UNIQUE KEY uk_hall_of_fame_entry_slug (slug),
    KEY idx_hall_of_fame_entry_public (status, act_year, act_semester, display_order),
    KEY idx_hall_of_fame_entry_user (user_id),
    CONSTRAINT fk_hall_of_fame_entry_user
        FOREIGN KEY (user_id) REFERENCES tb_user (user_id) ON DELETE SET NULL,
    CONSTRAINT ck_hall_of_fame_entry_semester CHECK (act_semester IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_hall_of_fame_badge (
    entry_id INT NOT NULL,
    badge VARCHAR(30) NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (entry_id, display_order),
    CONSTRAINT fk_hall_of_fame_badge_entry
        FOREIGN KEY (entry_id) REFERENCES tb_hall_of_fame_entry (entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tb_hall_of_fame_achievement (
    achievement_id INT NOT NULL AUTO_INCREMENT,
    entry_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    link_url VARCHAR(300) NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (achievement_id),
    KEY idx_hall_of_fame_achievement_order (entry_id, display_order),
    CONSTRAINT fk_hall_of_fame_achievement_entry
        FOREIGN KEY (entry_id) REFERENCES tb_hall_of_fame_entry (entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
