UPDATE tb_user
SET phone_num = CASE
    WHEN REGEXP_REPLACE(phone_num, '[^0-9]', '') REGEXP '^0082' THEN
        CONCAT('0', SUBSTRING(REGEXP_REPLACE(phone_num, '[^0-9]', ''), 5))
    WHEN REGEXP_REPLACE(phone_num, '[^0-9]', '') REGEXP '^82' THEN
        CONCAT('0', SUBSTRING(REGEXP_REPLACE(phone_num, '[^0-9]', ''), 3))
    ELSE REGEXP_REPLACE(phone_num, '[^0-9]', '')
END
WHERE phone_num IS NOT NULL
  AND phone_num <> CASE
      WHEN REGEXP_REPLACE(phone_num, '[^0-9]', '') REGEXP '^0082' THEN
          CONCAT('0', SUBSTRING(REGEXP_REPLACE(phone_num, '[^0-9]', ''), 5))
      WHEN REGEXP_REPLACE(phone_num, '[^0-9]', '') REGEXP '^82' THEN
          CONCAT('0', SUBSTRING(REGEXP_REPLACE(phone_num, '[^0-9]', ''), 3))
      ELSE REGEXP_REPLACE(phone_num, '[^0-9]', '')
  END;

ALTER TABLE tb_user
    ADD CONSTRAINT chk_tb_user_phone_num_normalized
        CHECK (phone_num IS NULL OR phone_num REGEXP '^[0-9]+$'),
    ADD CONSTRAINT uk_tb_user_phone_num UNIQUE (phone_num);
