ALTER TABLE tb_member_semester_check
    ADD COLUMN registration_withdrawn bit(1) NOT NULL DEFAULT b'0' AFTER google_form_submitted;
