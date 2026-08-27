-- Run once against the release database before deploying this application version.
-- Existing teams predate competition_type and are treated as HACKATHON teams.
-- Before adding the global unique key, set each historic event_round to its actual cumulative round.
-- For example, after assigning historic rows through 16, the next created event receives 17.

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
