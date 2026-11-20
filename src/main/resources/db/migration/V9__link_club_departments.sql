-- 기존 운영진 이력의 부서명으로 공통 부서를 만든다.
-- 이전 모집 회차는 테스트 데이터이므로 이름과 관계를 이관하지 않는다.

CREATE TABLE tb_club_department (
    club_department_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (club_department_id),
    UNIQUE KEY uk_club_department_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE tb_forif_team
    MODIFY club_department VARCHAR(50) NULL,
    ADD COLUMN club_department_id BIGINT NULL;

ALTER TABLE tb_staff_recruitment_department
    ADD COLUMN club_department_id BIGINT NULL;

-- 대소문자만 다른 이름이 있으면 한 부서로 합치되 대표 표기는 일정하게 고른다.
INSERT INTO tb_club_department (name)
SELECT MIN(TRIM(club_department) COLLATE utf8mb4_bin)
FROM tb_forif_team
WHERE club_department IS NOT NULL AND TRIM(club_department) <> ''
GROUP BY TRIM(club_department) COLLATE utf8mb4_0900_ai_ci;

-- 이력의 기존 부서명은 당시 표시값으로 보존하고 ID만 연결한다.
UPDATE tb_forif_team AS team
JOIN tb_club_department AS department
    ON department.name = TRIM(team.club_department) COLLATE utf8mb4_0900_ai_ci
SET team.club_department_id = department.club_department_id;

ALTER TABLE tb_forif_team
    ADD KEY idx_forif_team_club_department (club_department_id),
    ADD CONSTRAINT fk_forif_team_club_department
        FOREIGN KEY (club_department_id) REFERENCES tb_club_department (club_department_id);

-- 기존 모집 행은 NULL로 남긴다. 신규 모집은 애플리케이션에서 부서를 생성·연결한다.
ALTER TABLE tb_staff_recruitment_department
    ADD KEY idx_staff_recruitment_club_department (club_department_id),
    ADD CONSTRAINT fk_staff_recruitment_club_department
        FOREIGN KEY (club_department_id) REFERENCES tb_club_department (club_department_id);
