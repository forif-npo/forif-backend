-- Run once against the release database before deploying the tech-stack normalization change.
-- Preserve the existing name column because it is the user-facing display label.

ALTER TABLE tb_hackathon_submission_tech_stack
    ADD COLUMN normalized_name VARCHAR(50) NULL AFTER name;

UPDATE tb_hackathon_submission_tech_stack
SET normalized_name = LOWER(REGEXP_REPLACE(TRIM(name), '[[:space:]]+', ' '))
WHERE normalized_name IS NULL;

ALTER TABLE tb_hackathon_submission_tech_stack
    MODIFY COLUMN normalized_name VARCHAR(50) NOT NULL,
    ADD INDEX idx_hackathon_submission_tech_stack_normalized (submission_id, normalized_name);
