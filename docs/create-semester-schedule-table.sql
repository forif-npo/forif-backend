-- 학기 모집 단계 기간 (FOR-115)
--
-- 로컬(local 프로파일)만 ddl-auto: update라 자동 생성된다.
-- dev 서버와 운영 서버는 release 프로파일 = ddl-auto: validate 이므로
-- JAR을 교체하기 "전에" 이 스크립트를 먼저 돌려야 한다.
-- 순서를 바꾸면 검증 실패로 기동에 실패하고 컨테이너가 재시작을 반복한다.
--
-- [실행 전 확인] 기존 테이블을 버린다. 운영은 비어 있음을 확인했으나 dev는 미확인이다.
--   SELECT COUNT(*) FROM tb_semester_schedule;
-- 0이 아니면 담당자에게 확인할 것.
--
-- 기존 스키마(schedule_type varchar, scheduled_at)를 ALTER로 살리지 않고 버리는 이유:
-- scheduled_at이 NOT NULL인 채 남으면 엔티티가 매핑하지 않아 INSERT에서 빠지고,
-- 기동은 성공하는데 첫 저장에서 "Field 'scheduled_at' doesn't have a default value"로
-- 실패한다. 배포는 통과하고 사용 시점에 터지는 가장 나쁜 형태다.

DROP TABLE IF EXISTS tb_semester_schedule;

CREATE TABLE tb_semester_schedule (
    schedule_id  BIGINT      NOT NULL AUTO_INCREMENT,
    act_year     INT         NOT NULL,
    act_semester INT         NOT NULL,
    -- SemesterPhase enum name
    -- MENTOR_RECRUIT / MENTOR_REVIEW / MENTEE_RECRUIT / MENTEE_REVIEW
    phase        VARCHAR(30) NOT NULL,
    -- 반열림 구간 [starts_at, ends_at). Asia/Seoul 벽시계 기준.
    -- "3월 8일까지"는 2026-03-09 00:00:00 으로 넣는다.
    starts_at    DATETIME(6) NOT NULL,
    ends_at      DATETIME(6) NOT NULL,
    -- 마지막으로 이 기간을 수정한 회장단 학번
    updated_by   BIGINT      NULL,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (schedule_id),
    -- 학기·단계당 1행. 전체 교체(PUT)의 멱등성을 DB가 보장한다.
    UNIQUE KEY uk_semester_schedule_phase (act_year, act_semester, phase)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 초기 데이터는 넣지 않는다.
-- 행이 없는 상태 = 해당 단계 상시 개방(fail-open)이며,
-- 배포 직후 동작이 지금과 동일하게 유지된다는 뜻이다.
-- 회장단이 어드민 학기 관리 화면에서 직접 채운다.
