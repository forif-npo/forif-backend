-- Deploy this once to the production database before deploying the backend.
-- There is no existing product data, so no backfill is necessary.
-- The application-status enum is replaced, and the operation-status enum is added.
ALTER TABLE tb_product
    MODIFY COLUMN status ENUM('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL,
    ADD COLUMN operation_status ENUM('LIVE', 'PAUSED') NULL AFTER status;
