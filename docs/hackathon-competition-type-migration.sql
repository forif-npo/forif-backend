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

ALTER TABLE tb_hackathon_event
    ADD CONSTRAINT uk_hackathon_event_round UNIQUE (event_round),
    ADD CONSTRAINT uk_hackathon_event_semester UNIQUE (held_year, held_semester);

ALTER TABLE tb_hackathon_team
    ADD COLUMN competition_type VARCHAR(20) NULL AFTER description;

UPDATE tb_hackathon_team
SET competition_type = 'HACKATHON'
WHERE competition_type IS NULL;

ALTER TABLE tb_hackathon_team
    MODIFY COLUMN competition_type VARCHAR(20) NOT NULL;
