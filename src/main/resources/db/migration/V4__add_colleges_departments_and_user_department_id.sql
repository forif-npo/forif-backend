CREATE TABLE tb_college (
    college_id BIGINT NOT NULL AUTO_INCREMENT,
    college_name VARCHAR(100) NOT NULL,
    source_code VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (college_id),
    UNIQUE KEY uk_college_name (college_name),
    UNIQUE KEY uk_college_source_code (source_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_department (
    department_id BIGINT NOT NULL AUTO_INCREMENT,
    college_id BIGINT NOT NULL,
    department_name VARCHAR(100) NOT NULL,
    source_code VARCHAR(20) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (department_id),
    UNIQUE KEY uk_department_name (department_name),
    UNIQUE KEY uk_department_source_code (source_code),
    KEY idx_department_college_id (college_id),
    CONSTRAINT fk_department_college
        FOREIGN KEY (college_id) REFERENCES tb_college (college_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tb_college (college_name, source_code, created_at, updated_at) VALUES
    ('간호대학', 'H0004811', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('경영대학', 'H0002889', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('경제금융대학', 'H0002928', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('공과대학', 'H0002449', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('국제대학', 'H0005054', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('기술혁신대학', 'H0005236', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('사범대학', 'H0002702', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('사회과학대학', 'H0002821', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('생활과학대학', 'H0002905', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('예술·체육대학', 'H0003586', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('음악대학', 'H0002731', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('의과대학', 'H0002589', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('인문과학대학', 'H0002783', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('자연과학대학', 'H0002844', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('정책과학대학', 'H0002979', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('한양YK인터칼리지', 'H0005243', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

INSERT INTO tb_department (college_id, department_name, source_code, created_at, updated_at)
SELECT c.college_id, d.department_name, d.source_code, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM tb_college c
JOIN (
    SELECT '간호대학' college_name, '간호학과' department_name, 'H0004816' source_code UNION ALL
    SELECT '경영대학', '경영학부', 'H0002894' UNION ALL
    SELECT '경영대학', '파이낸스경영학과', 'H0002895' UNION ALL
    SELECT '경제금융대학', '경제금융학부', 'H0002933' UNION ALL
    SELECT '공과대학', '건설환경공학과', 'H0002517' UNION ALL
    SELECT '공과대학', '건축공학부', 'H0002521' UNION ALL
    SELECT '공과대학', '건축학부', 'H0002886' UNION ALL
    SELECT '공과대학', '기계공학부', 'H0002540' UNION ALL
    SELECT '공과대학', '데이터사이언스학부', 'H0004707' UNION ALL
    SELECT '공과대학', '데이터사이언스전공', 'H0004744' UNION ALL
    SELECT '공과대학', '심리뇌과학전공', 'H0004743' UNION ALL
    SELECT '공과대학', '도시공학과', 'H0002570' UNION ALL
    SELECT '공과대학', '미래자동차공학과', 'H0002541' UNION ALL
    SELECT '공과대학', '반도체공학과', 'H0004761' UNION ALL
    SELECT '공과대학', '산업공학과', 'H0002563' UNION ALL
    SELECT '공과대학', '생명공학과', 'H0003831' UNION ALL
    SELECT '공과대학', '신소재공학부', 'H0002581' UNION ALL
    SELECT '공과대학', '에너지공학과', 'H0002530' UNION ALL
    SELECT '공과대학', '원자력공학과', 'H0002558' UNION ALL
    SELECT '공과대학', '유기나노공학과', 'H0002525' UNION ALL
    SELECT '공과대학', '융합전자공학부', 'H0002575' UNION ALL
    SELECT '공과대학', '자원환경공학과', 'H0002560' UNION ALL
    SELECT '공과대학', '전기·생체공학부', 'H0002574' UNION ALL
    SELECT '공과대학', '바이오메디컬공학전공', 'H0004701' UNION ALL
    SELECT '공과대학', '전기공학전공', 'H0002520' UNION ALL
    SELECT '공과대학', '정보시스템학과', 'H0002867' UNION ALL
    SELECT '공과대학', '컴퓨터소프트웨어학부', 'H0004134' UNION ALL
    SELECT '공과대학', '화학공학과', 'H0003830' UNION ALL
    SELECT '국제대학', '국제학부', 'H0005063' UNION ALL
    SELECT '국제대학', '글로벌콘텐츠융합학부', 'H0005064' UNION ALL
    SELECT '기술혁신대학', '산업융합학부', 'H0005238' UNION ALL
    SELECT '기술혁신대학', '경영공학전공', 'H0005239' UNION ALL
    SELECT '기술혁신대학', '정보공학전공', 'H0005240' UNION ALL
    SELECT '사범대학', '교육공학과', 'H0002723' UNION ALL
    SELECT '사범대학', '교육학과', 'H0002718' UNION ALL
    SELECT '사범대학', '국어교육과', 'H0002719' UNION ALL
    SELECT '사범대학', '수학교육과', 'H0002725' UNION ALL
    SELECT '사범대학', '영어교육과', 'H0002720' UNION ALL
    SELECT '사범대학', '응용미술교육과', 'H0002722' UNION ALL
    SELECT '사회과학대학', '관광학부', 'H0002839' UNION ALL
    SELECT '사회과학대학', '미디어커뮤니케이션학과', 'H0003665' UNION ALL
    SELECT '사회과학대학', '사회학과', 'H0003664' UNION ALL
    SELECT '사회과학대학', '정치외교학과', 'H0003663' UNION ALL
    SELECT '생활과학대학', '식품영양학과', 'H0003584' UNION ALL
    SELECT '생활과학대학', '실내건축디자인학과', 'H0003585' UNION ALL
    SELECT '생활과학대학', '의류학과', 'H0003583' UNION ALL
    SELECT '예술·체육대학', '무용학과', 'H0003590' UNION ALL
    SELECT '예술·체육대학', '스포츠산업과학부', 'H0004698' UNION ALL
    SELECT '예술·체육대학', '스포츠매니지먼트전공', 'H0004700' UNION ALL
    SELECT '예술·체육대학', '스포츠사이언스전공', 'H0004708' UNION ALL
    SELECT '예술·체육대학', '연극영화학과', 'H0003589' UNION ALL
    SELECT '음악대학', '관현악과', 'H0002740' UNION ALL
    SELECT '음악대학', '국악과', 'H0002741' UNION ALL
    SELECT '음악대학', '성악과', 'H0002737' UNION ALL
    SELECT '음악대학', '작곡과', 'H0002738' UNION ALL
    SELECT '음악대학', '피아노과', 'H0002739' UNION ALL
    SELECT '의과대학', '의예과', 'H0002599' UNION ALL
    SELECT '의과대학', '의학과', NULL UNION ALL
    SELECT '인문과학대학', '국어국문학과', 'H0002794' UNION ALL
    SELECT '인문과학대학', '독어독문학과', 'H0002801' UNION ALL
    SELECT '인문과학대학', '사학과', 'H0002814' UNION ALL
    SELECT '인문과학대학', '영어영문학과', 'H0002800' UNION ALL
    SELECT '인문과학대학', '중어중문학과', 'H0002809' UNION ALL
    SELECT '인문과학대학', '철학과', 'H0002815' UNION ALL
    SELECT '자연과학대학', '물리학과', 'H0003561' UNION ALL
    SELECT '자연과학대학', '생명과학과', 'H0003563' UNION ALL
    SELECT '자연과학대학', '수학과', 'H0003560' UNION ALL
    SELECT '자연과학대학', '화학과', 'H0003562' UNION ALL
    SELECT '정책과학대학', '정책학과', 'H0002983' UNION ALL
    SELECT '정책과학대학', '행정학과', 'H0003666' UNION ALL
    SELECT '한양YK인터칼리지', '한양인터칼리지학부', 'H0005245'
) d ON d.college_name = c.college_name;

ALTER TABLE tb_user
    ADD COLUMN department_id BIGINT NULL,
    ADD KEY idx_user_department_id (department_id),
    ADD CONSTRAINT fk_user_department
        FOREIGN KEY (department_id) REFERENCES tb_department (department_id);

UPDATE tb_user u
JOIN tb_department d ON d.department_name = TRIM(u.department)
SET u.department_id = d.department_id;

UPDATE tb_user u
JOIN tb_department d ON d.department_name = '전기·생체공학부'
SET u.department_id = d.department_id
WHERE TRIM(u.department) = '전기생체공학부';

UPDATE tb_user u
JOIN tb_department d ON d.department_name = '한양인터칼리지학부'
SET u.department_id = d.department_id
WHERE TRIM(u.department) = '한양인터칼리지';
