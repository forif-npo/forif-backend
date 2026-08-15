-- 멘토 활동 확인서 발급 이력 테이블
-- release 프로파일은 ddl-auto: validate 이므로 배포 전에 실행해야 합니다.

CREATE TABLE IF NOT EXISTS tb_mentor_confirmation (
    mentor_confirmation_id BIGINT       NOT NULL AUTO_INCREMENT,
    study_id               INT          NOT NULL,
    mentor_id              BIGINT       NOT NULL,
    confirmation_object_key VARCHAR(300) NOT NULL,
    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,
    PRIMARY KEY (mentor_confirmation_id),
    UNIQUE KEY uk_mentor_confirmation_study_mentor (study_id, mentor_id),
    CONSTRAINT fk_mentor_confirmation_study
        FOREIGN KEY (study_id) REFERENCES tb_study (study_id),
    CONSTRAINT fk_mentor_confirmation_user
        FOREIGN KEY (mentor_id) REFERENCES tb_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
