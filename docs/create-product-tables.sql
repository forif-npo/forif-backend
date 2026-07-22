-- 프로덕트 쇼케이스 테이블 (FOR-105)
-- local/dev 프로필은 ddl-auto: update 로 자동 생성되므로,
-- release(validate) 배포 전에만 수동 실행이 필요하다.

CREATE TABLE IF NOT EXISTS tb_product (
    product_id           INT          NOT NULL AUTO_INCREMENT,
    slug                 VARCHAR(30)  NOT NULL,
    name                 VARCHAR(100) NOT NULL,
    one_liner            VARCHAR(200) NOT NULL,
    description          TEXT         NULL,
    status               VARCHAR(20)  NOT NULL,
    source_type          VARCHAR(20)  NOT NULL,
    source_label         VARCHAR(100) NULL,
    tags                 VARCHAR(200) NULL,
    tech_stack           VARCHAR(300) NULL,
    service_url          VARCHAR(300) NULL,
    github_url           VARCHAR(300) NULL,
    thumbnail_object_key VARCHAR(300) NULL,
    act_year             INT          NOT NULL,
    applicant_user_id    BIGINT       NOT NULL,
    reject_reason        VARCHAR(500) NULL,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (product_id),
    UNIQUE KEY uk_product_slug (slug),
    CONSTRAINT fk_product_applicant FOREIGN KEY (applicant_user_id) REFERENCES tb_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS tb_product_member (
    product_member_id BIGINT      NOT NULL AUTO_INCREMENT,
    product_id        INT         NOT NULL,
    user_name         VARCHAR(50) NOT NULL,
    role_label        VARCHAR(50) NOT NULL,
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (product_member_id),
    CONSTRAINT fk_product_member_product FOREIGN KEY (product_id) REFERENCES tb_product (product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
