-- Run once against the release database before deploying this application version.
-- Existing teams predate competition_type and are treated as HACKATHON teams.
-- hackathon_id=1 is the sole modeled event (2026-1) and represents the 16th
-- historical event. Preserve the real cumulative round before enforcing the
-- global unique constraint. The next created event will receive 17.
UPDATE tb_hackathon_event
SET event_round = 16
WHERE hackathon_id = 1;

SELECT hackathon_id, held_year, held_semester, event_round, title, deleted_at
FROM tb_hackathon_event
ORDER BY hackathon_id;

-- MySQL unique indexes allow multiple NULL values. These generated columns keep
-- deleted rows out of the key while still allowing only one active event per semester.
ALTER TABLE tb_hackathon_event
    ADD COLUMN active_held_year INT GENERATED ALWAYS AS (
        IF(deleted_at IS NULL, held_year, NULL)
    ) STORED,
    ADD COLUMN active_held_semester INT GENERATED ALWAYS AS (
        IF(deleted_at IS NULL, held_semester, NULL)
    ) STORED;

ALTER TABLE tb_hackathon_event
    ADD CONSTRAINT uk_hackathon_event_round UNIQUE (event_round),
    ADD CONSTRAINT uk_hackathon_event_active_semester
        UNIQUE (active_held_year, active_held_semester);

ALTER TABLE tb_hackathon_team
    ADD COLUMN competition_type VARCHAR(20) NULL AFTER description;

UPDATE tb_hackathon_team
SET competition_type = 'HACKATHON'
WHERE competition_type IS NULL;

ALTER TABLE tb_hackathon_team
    MODIFY COLUMN competition_type VARCHAR(20) NOT NULL;
