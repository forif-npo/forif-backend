-- Run once against the release database before deploying this application version.
-- Existing events predate competition_type, so they are all treated as HACKATHON.

ALTER TABLE tb_hackathon_event
    ADD COLUMN competition_type VARCHAR(20) NULL AFTER event_round;

UPDATE tb_hackathon_event
SET competition_type = 'HACKATHON'
WHERE competition_type IS NULL;

ALTER TABLE tb_hackathon_event
    MODIFY COLUMN competition_type VARCHAR(20) NOT NULL;

-- The previous unique key was (held_year, held_semester, event_round).
-- Its generated name differs by database, so resolve and remove it dynamically.
SET @old_unique_index = (
    SELECT index_name
    FROM (
        SELECT index_name
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'tb_hackathon_event'
          AND non_unique = 0
        GROUP BY index_name
        HAVING COUNT(*) = 3
           AND SUM(column_name = 'held_year') = 1
           AND SUM(column_name = 'held_semester') = 1
           AND SUM(column_name = 'event_round') = 1
    ) AS matching_indexes
    LIMIT 1
);

SET @drop_old_unique_index = IF(
    @old_unique_index IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE tb_hackathon_event DROP INDEX `', @old_unique_index, '`')
);
PREPARE drop_old_unique_index FROM @drop_old_unique_index;
EXECUTE drop_old_unique_index;
DEALLOCATE PREPARE drop_old_unique_index;

ALTER TABLE tb_hackathon_event
    ADD CONSTRAINT uk_hackathon_event_semester_round_type
        UNIQUE (held_year, held_semester, event_round, competition_type);
