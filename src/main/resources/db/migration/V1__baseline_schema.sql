-- FORIF 운영 스키마 베이스라인 (forif @ MySQL 8.0.46, 33 테이블)
-- Flyway 도입 시점의 기준 스키마이며, 이후 변경은 V2 이상으로만 추가한다.
-- 기존 DB는 baseline-on-migrate 로 이 스크립트를 실행하지 않고 이력만 기록한다.

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `tb_active_semester` (
  `active_semester_id` int NOT NULL,
  `act_year` int NOT NULL,
  `act_semester` int NOT NULL,
  `updated_by` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`active_semester_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `tb_forif_team` (
  `act_semester` int NOT NULL,
  `act_year` int NOT NULL,
  `graduate_year` int DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `forif_team_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `club_department` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_title` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `intro_tag` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `self_intro` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`forif_team_id`),
  UNIQUE KEY `UKr096sbcix199t5syp0y0ungrc` (`act_year`,`act_semester`,`user_id`),
  KEY `FKj85uj1sxw0ok9khckq4uyu4fr` (`user_id`),
  CONSTRAINT `FKj85uj1sxw0ok9khckq4uyu4fr` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_hackathon` (
  `hackathon_team_id` int NOT NULL AUTO_INCREMENT,
  `held_semester` int NOT NULL,
  `held_year` int NOT NULL,
  `team_id` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `project_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `result_url` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`hackathon_team_id`),
  UNIQUE KEY `UKpyyyaeg9snefytuvvdghwka7h` (`held_year`,`held_semester`,`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_hackathon_award` (
  `award_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `award_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `award_rank` int DEFAULT NULL,
  `hackathon_id` bigint NOT NULL,
  `hackathon_team_id` bigint NOT NULL,
  PRIMARY KEY (`award_id`),
  UNIQUE KEY `UKi6778ojh5y56nb1juva47mnt9` (`hackathon_id`,`hackathon_team_id`,`award_name`),
  KEY `FK36t04psvw8c4fu9ghh6a3crk0` (`hackathon_team_id`),
  CONSTRAINT `FK36t04psvw8c4fu9ghh6a3crk0` FOREIGN KEY (`hackathon_team_id`) REFERENCES `tb_hackathon_team` (`hackathon_team_id`),
  CONSTRAINT `FKcl4ssqodka9hkvmvkk4a634k0` FOREIGN KEY (`hackathon_id`) REFERENCES `tb_hackathon_event` (`hackathon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_hackathon_evaluation` (
  `evaluation_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `evaluated_at` datetime(6) NOT NULL,
  `evaluator_type` enum('ADMIN','PARTICIPANT') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_score` decimal(8,2) NOT NULL,
  `evaluator_id` bigint NOT NULL,
  `hackathon_id` bigint NOT NULL,
  `target_team_id` bigint NOT NULL,
  PRIMARY KEY (`evaluation_id`),
  UNIQUE KEY `UKeh8onussokxyvwtw21gg7gcbi` (`hackathon_id`,`target_team_id`,`evaluator_id`),
  KEY `FK3yr8a72ujqfth1lcypmsn31o7` (`evaluator_id`),
  KEY `FKt7duh0236kasg7firri3kjvq7` (`target_team_id`),
  CONSTRAINT `FK3yr8a72ujqfth1lcypmsn31o7` FOREIGN KEY (`evaluator_id`) REFERENCES `tb_user` (`user_id`),
  CONSTRAINT `FK9qp1cpolaec5jlj83x6w1mgei` FOREIGN KEY (`hackathon_id`) REFERENCES `tb_hackathon_event` (`hackathon_id`),
  CONSTRAINT `FKt7duh0236kasg7firri3kjvq7` FOREIGN KEY (`target_team_id`) REFERENCES `tb_hackathon_team` (`hackathon_team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_hackathon_evaluation_criterion` (
  `criterion_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `display_order` int NOT NULL,
  `max_score` int NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `weight` decimal(5,2) NOT NULL,
  `hackathon_id` bigint NOT NULL,
  PRIMARY KEY (`criterion_id`),
  UNIQUE KEY `UKeu54l44ov4tb326my2ohl3xtu` (`hackathon_id`,`display_order`),
  UNIQUE KEY `UKfeupvk4h0ewsqdnmkruosq327` (`hackathon_id`,`name`),
  CONSTRAINT `FK9w2jn32eakf48ogf2ihp3f2t1` FOREIGN KEY (`hackathon_id`) REFERENCES `tb_hackathon_event` (`hackathon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_hackathon_evaluation_score` (
  `evaluation_score_id` bigint NOT NULL AUTO_INCREMENT,
  `score` int NOT NULL,
  `criterion_id` bigint NOT NULL,
  `evaluation_id` bigint NOT NULL,
  PRIMARY KEY (`evaluation_score_id`),
  UNIQUE KEY `UKft7nhtgv0afhtva498p74hm7q` (`evaluation_id`,`criterion_id`),
  KEY `FK610vkxtut16y3jaay3nuaf25` (`criterion_id`),
  CONSTRAINT `FK610vkxtut16y3jaay3nuaf25` FOREIGN KEY (`criterion_id`) REFERENCES `tb_hackathon_evaluation_criterion` (`criterion_id`),
  CONSTRAINT `FKdwt744e78nernw0963sd00qp0` FOREIGN KEY (`evaluation_id`) REFERENCES `tb_hackathon_evaluation` (`evaluation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_hackathon_event` (
  `hackathon_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `ends_at` datetime(6) NOT NULL,
  `event_round` int NOT NULL,
  `held_semester` int NOT NULL,
  `held_year` int NOT NULL,
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recruit_ends_at` datetime(6) DEFAULT NULL,
  `recruit_starts_at` datetime(6) DEFAULT NULL,
  `starts_at` datetime(6) NOT NULL,
  `status` enum('ENDED','IN_PROGRESS','JUDGING','RECRUITING','TEAM_BUILDING') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `team_building_ends_at` datetime(6) DEFAULT NULL,
  `team_building_starts_at` datetime(6) DEFAULT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `active_held_year` int GENERATED ALWAYS AS (if((`deleted_at` is null),`held_year`,NULL)) STORED,
  `active_held_semester` int GENERATED ALWAYS AS (if((`deleted_at` is null),`held_semester`,NULL)) STORED,
  PRIMARY KEY (`hackathon_id`),
  UNIQUE KEY `UKafo62mke1m347a969pru2au9a` (`held_year`,`held_semester`,`event_round`),
  UNIQUE KEY `uk_hackathon_event_round` (`event_round`),
  UNIQUE KEY `uk_hackathon_event_active_semester` (`active_held_year`,`active_held_semester`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_hackathon_join_request` (
  `join_request_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `status` enum('APPROVED','CANCELED','PENDING','REJECTED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `hackathon_id` bigint NOT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  `hackathon_team_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`join_request_id`),
  KEY `FKd2el517j2lsspl3r3qpyr594x` (`hackathon_id`),
  KEY `FK7ksqjno21mmxanx1k9ohld6tm` (`reviewed_by`),
  KEY `FK3xbm47m11pqwgdkdgiaj3cooi` (`hackathon_team_id`),
  KEY `FKcq2g3968ahod0ntdk7sp4r93q` (`user_id`),
  CONSTRAINT `FK3xbm47m11pqwgdkdgiaj3cooi` FOREIGN KEY (`hackathon_team_id`) REFERENCES `tb_hackathon_team` (`hackathon_team_id`),
  CONSTRAINT `FK7ksqjno21mmxanx1k9ohld6tm` FOREIGN KEY (`reviewed_by`) REFERENCES `tb_user` (`user_id`),
  CONSTRAINT `FKcq2g3968ahod0ntdk7sp4r93q` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`),
  CONSTRAINT `FKd2el517j2lsspl3r3qpyr594x` FOREIGN KEY (`hackathon_id`) REFERENCES `tb_hackathon_event` (`hackathon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_hackathon_participant` (
  `participant_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `canceled_at` datetime(6) DEFAULT NULL,
  `registered_at` datetime(6) NOT NULL,
  `status` enum('CANCELED','REGISTERED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `hackathon_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`participant_id`),
  UNIQUE KEY `UK89pfor39ofjxwd1p6vosv94ym` (`hackathon_id`,`user_id`),
  KEY `FKbhx9x67jrnxsr5vgi9slnhj9m` (`user_id`),
  CONSTRAINT `FKbhx9x67jrnxsr5vgi9slnhj9m` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`),
  CONSTRAINT `FKi30m86ydtbv48hpocgbso4mff` FOREIGN KEY (`hackathon_id`) REFERENCES `tb_hackathon_event` (`hackathon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_hackathon_submission` (
  `submission_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deploy_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `github_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `presentation_file` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `hackathon_id` bigint NOT NULL,
  `hackathon_team_id` bigint NOT NULL,
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`submission_id`),
  UNIQUE KEY `UK2vcb235xb9tqs089n4nk1o187` (`hackathon_id`,`hackathon_team_id`),
  KEY `FKnchvl5dxlwkvl439j7x0xp4y3` (`hackathon_team_id`),
  CONSTRAINT `FK9knoimrvevf04cbguvfoo6xdg` FOREIGN KEY (`hackathon_id`) REFERENCES `tb_hackathon_event` (`hackathon_id`),
  CONSTRAINT `FKnchvl5dxlwkvl439j7x0xp4y3` FOREIGN KEY (`hackathon_team_id`) REFERENCES `tb_hackathon_team` (`hackathon_team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_hackathon_submission_tech_stack` (
  `tech_stack_id` bigint NOT NULL AUTO_INCREMENT,
  `display_order` int NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `normalized_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `submission_id` bigint NOT NULL,
  PRIMARY KEY (`tech_stack_id`),
  UNIQUE KEY `UKoh3cg6daorbvtct2u5d1wrst` (`submission_id`,`display_order`),
  KEY `idx_hackathon_submission_tech_stack_normalized` (`submission_id`,`normalized_name`),
  CONSTRAINT `FKpb3nmu2oc02aewpcpatomsq8c` FOREIGN KEY (`submission_id`) REFERENCES `tb_hackathon_submission` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_hackathon_team` (
  `hackathon_team_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `competition_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HACKATHON',
  `max_members` int DEFAULT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('CONFIRMED','DISBANDED','FORMING') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `topic` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `hackathon_id` bigint NOT NULL,
  `leader_id` bigint NOT NULL,
  PRIMARY KEY (`hackathon_team_id`),
  UNIQUE KEY `UKt2u6hnp9r9ftpxgorij68ahmv` (`hackathon_id`,`name`),
  KEY `FKbsbynjfk7w8m1mtpytts4fxi8` (`leader_id`),
  CONSTRAINT `FKbsbynjfk7w8m1mtpytts4fxi8` FOREIGN KEY (`leader_id`) REFERENCES `tb_user` (`user_id`),
  CONSTRAINT `FKn8blmloddh9poqbbxmusestu5` FOREIGN KEY (`hackathon_id`) REFERENCES `tb_hackathon_event` (`hackathon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_hackathon_team_member` (
  `team_member_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `joined_at` datetime(6) NOT NULL,
  `role` enum('LEADER','MEMBER') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `hackathon_id` bigint NOT NULL,
  `hackathon_team_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`team_member_id`),
  UNIQUE KEY `UK8cq4o1mxbr3crpxy1v8fhvvpx` (`hackathon_id`,`user_id`),
  UNIQUE KEY `UKgxtrv5klvjtwu8rtkx56cuh1f` (`hackathon_team_id`,`user_id`),
  KEY `FKafqabvbl3yagaebs5htx2mjxf` (`user_id`),
  CONSTRAINT `FK455tk8mbaspd9nc0yu72y6pe7` FOREIGN KEY (`hackathon_id`) REFERENCES `tb_hackathon_event` (`hackathon_id`),
  CONSTRAINT `FKafqabvbl3yagaebs5htx2mjxf` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`),
  CONSTRAINT `FKhwxdkdgt30g9x89jg8v33lsh` FOREIGN KEY (`hackathon_team_id`) REFERENCES `tb_hackathon_team` (`hackathon_team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_member_semester_check` (
  `member_semester_check_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `act_year` int NOT NULL,
  `act_semester` int NOT NULL,
  `dues_paid` bit(1) NOT NULL DEFAULT b'0',
  `google_form_submitted` bit(1) NOT NULL DEFAULT b'0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`member_semester_check_id`),
  UNIQUE KEY `uk_member_semester_check` (`user_id`,`act_year`,`act_semester`),
  CONSTRAINT `fk_member_semester_check_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `tb_mentor_confirmation` (
  `mentor_confirmation_id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` int NOT NULL,
  `mentor_id` bigint NOT NULL,
  `confirmation_object_key` varchar(300) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`mentor_confirmation_id`),
  UNIQUE KEY `uk_mentor_confirmation_study_mentor` (`study_id`,`mentor_id`),
  KEY `fk_mentor_confirmation_user` (`mentor_id`),
  CONSTRAINT `fk_mentor_confirmation_study` FOREIGN KEY (`study_id`) REFERENCES `tb_study` (`study_id`),
  CONSTRAINT `fk_mentor_confirmation_user` FOREIGN KEY (`mentor_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `tb_mentor_study` (
  `mentor_num` int DEFAULT NULL,
  `study_id` int NOT NULL,
  `mentor_id` bigint NOT NULL,
  PRIMARY KEY (`study_id`,`mentor_id`),
  KEY `FKeoysvbemg9mjhm9q8bh1rtdhj` (`mentor_id`),
  CONSTRAINT `FKb4s17je50hbpn5q9sjk5e0ytt` FOREIGN KEY (`study_id`) REFERENCES `tb_study` (`study_id`),
  CONSTRAINT `FKeoysvbemg9mjhm9q8bh1rtdhj` FOREIGN KEY (`mentor_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_post` (
  `post_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `post_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tag` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` varchar(5000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`post_id`),
  KEY `FKhx7a7k3pf66vpddqg5pr12anw` (`user_id`),
  CONSTRAINT `FKhx7a7k3pf66vpddqg5pr12anw` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_post_file` (
  `file_num` int NOT NULL,
  `post_id` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `post_file_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `file_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_url` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`post_file_id`),
  UNIQUE KEY `UK8snpply6f9vn8t0pivtdb4o11` (`post_id`,`file_num`),
  CONSTRAINT `FK5hv4iy3q4k6n37ne1hw6xhfd3` FOREIGN KEY (`post_id`) REFERENCES `tb_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_product` (
  `product_id` int NOT NULL AUTO_INCREMENT,
  `slug` varchar(30) NOT NULL,
  `name` varchar(100) NOT NULL,
  `one_liner` varchar(200) NOT NULL,
  `description` text,
  `status` enum('PENDING','ACCEPTED','REJECTED') NOT NULL,
  `operation_status` enum('LIVE','PAUSED') DEFAULT NULL,
  `source_type` varchar(20) NOT NULL,
  `source_label` varchar(100) DEFAULT NULL,
  `tags` varchar(200) DEFAULT NULL,
  `tech_stack` varchar(300) DEFAULT NULL,
  `service_url` varchar(300) DEFAULT NULL,
  `github_url` varchar(300) DEFAULT NULL,
  `thumbnail_object_key` varchar(300) DEFAULT NULL,
  `act_year` int NOT NULL,
  `applicant_user_id` bigint NOT NULL,
  `reject_reason` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `uk_product_slug` (`slug`),
  KEY `fk_product_applicant` (`applicant_user_id`),
  CONSTRAINT `fk_product_applicant` FOREIGN KEY (`applicant_user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `tb_product_member` (
  `product_member_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` int NOT NULL,
  `user_name` varchar(50) NOT NULL,
  `role_label` varchar(50) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`product_member_id`),
  KEY `fk_product_member_product` (`product_id`),
  CONSTRAINT `fk_product_member_product` FOREIGN KEY (`product_id`) REFERENCES `tb_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `tb_semester_change_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `from_year` int NOT NULL,
  `from_semester` int NOT NULL,
  `to_year` int NOT NULL,
  `to_semester` int NOT NULL,
  `changed_by` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `tb_semester_schedule` (
  `schedule_id` bigint NOT NULL AUTO_INCREMENT,
  `act_year` int NOT NULL,
  `act_semester` int NOT NULL,
  `phase` varchar(30) NOT NULL,
  `starts_at` datetime(6) NOT NULL,
  `ends_at` datetime(6) NOT NULL,
  `updated_by` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`schedule_id`),
  UNIQUE KEY `uk_semester_schedule_phase` (`act_year`,`act_semester`,`phase`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `tb_staff_account` (
  `staff_account_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `affiliation` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('ADMIN','MENTOR') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `signature_object_key` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`staff_account_id`),
  UNIQUE KEY `uq_staff_account_user_role` (`user_id`,`role`),
  CONSTRAINT `FKlyibbcf3ry8jcsw08nxssrgsc` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_study` (
  `act_semester` int NOT NULL,
  `act_year` int NOT NULL,
  `capacity` int DEFAULT NULL,
  `difficulty` int DEFAULT NULL,
  `is_online` bit(1) DEFAULT NULL,
  `requires_interview` bit(1) DEFAULT NULL,
  `study_id` int NOT NULL AUTO_INCREMENT,
  `week_day` int DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `interview_date` datetime(6) DEFAULT NULL,
  `primary_mentor_id` bigint DEFAULT NULL,
  `secondary_mentor_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `end_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `location` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `location_detail` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `primary_mentor_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `secondary_mentor_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `start_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `study_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sub_title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `selection_criteria` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `img_url` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `one_liner` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `goal` varchar(3000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reject_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `explanation` varchar(5000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recruit_status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `thumbnail_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `study_status` enum('PENDING','APPROVED','STARTED','REJECTED','RE_APPLIED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `autonomous_flag` bit(1) DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`study_id`),
  UNIQUE KEY `uk_study_autonomous_semester` (`act_year`,`act_semester`,`autonomous_flag`),
  KEY `FKqc7ln6km49xbvptdfus23tf94` (`primary_mentor_id`),
  KEY `FK19xd82mvw8ppb39onmfkpr0i9` (`secondary_mentor_id`),
  CONSTRAINT `FK19xd82mvw8ppb39onmfkpr0i9` FOREIGN KEY (`secondary_mentor_id`) REFERENCES `tb_user` (`user_id`),
  CONSTRAINT `FKqc7ln6km49xbvptdfus23tf94` FOREIGN KEY (`primary_mentor_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_study_attendance` (
  `study_id` int NOT NULL,
  `week_num` int NOT NULL,
  `attendance_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `study_date` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attendance_status` enum('ABSENT','PRESENT') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`attendance_id`),
  UNIQUE KEY `UKgrpph4wd7brlx2h1lnmrmaqrp` (`study_id`,`user_id`,`week_num`),
  KEY `FKafj7x9m6wxv7qxp9ak15t2301` (`user_id`),
  CONSTRAINT `FK59hy6846eq6vqe86h3t8n51s1` FOREIGN KEY (`study_id`) REFERENCES `tb_study` (`study_id`),
  CONSTRAINT `FKafj7x9m6wxv7qxp9ak15t2301` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_study_plan` (
  `study_id` int NOT NULL,
  `week_num` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `date` datetime(6) DEFAULT NULL,
  `study_plan_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `section` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`study_plan_id`),
  UNIQUE KEY `UKmu78j3uge5e3365dwaodj0i6o` (`study_id`,`week_num`),
  CONSTRAINT `FKdx48dsqbvkfrgassyyoeh67tn` FOREIGN KEY (`study_id`) REFERENCES `tb_study` (`study_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_study_reference` (
  `study_id` int NOT NULL,
  `study_reference_id` binary(16) NOT NULL,
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `reference_type` enum('FILE','URL') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`study_reference_id`),
  KEY `FK973ivl8f3squs249qmdo0j5e1` (`study_id`),
  CONSTRAINT `FK973ivl8f3squs249qmdo0j5e1` FOREIGN KEY (`study_id`) REFERENCES `tb_study` (`study_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_study_tag` (
  `tag_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `UKiq9665ltxau61jwjov94whxaw` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_study_tag_mapping` (
  `study_id` int NOT NULL,
  `tag_id` bigint NOT NULL,
  KEY `FK5a65805isjbm8f5anfifax4os` (`tag_id`),
  KEY `FK9m08a3r9i80x5l0reu66sulwx` (`study_id`),
  CONSTRAINT `FK5a65805isjbm8f5anfifax4os` FOREIGN KEY (`tag_id`) REFERENCES `tb_study_tag` (`tag_id`),
  CONSTRAINT `FK9m08a3r9i80x5l0reu66sulwx` FOREIGN KEY (`study_id`) REFERENCES `tb_study` (`study_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_study_user` (
  `certificate_status` int DEFAULT NULL,
  `study_id` int NOT NULL,
  `user_id` bigint NOT NULL,
  `certificate_url` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`study_id`,`user_id`),
  KEY `FKp2h6earnee3pusgcdlpl7u9au` (`user_id`),
  CONSTRAINT `FKp2h6earnee3pusgcdlpl7u9au` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`),
  CONSTRAINT `FKqhepv93lr7e6uwcgp9t62djx` FOREIGN KEY (`study_id`) REFERENCES `tb_study` (`study_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_user` (
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `phone_num` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `img_url` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UK4vih17mube9j7cqyjlfbcrk4m` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `tb_user_apply` (
  `apply_semester` int NOT NULL,
  `apply_year` int NOT NULL,
  `pay_status` int DEFAULT NULL,
  `primary_study` int NOT NULL,
  `secondary_study` int DEFAULT NULL,
  `applier_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_apply_id` bigint NOT NULL AUTO_INCREMENT,
  `primary_intro` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `secondary_intro` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `primary_study_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `secondary_study_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `primary_status` enum('ACCEPT','PENDING','REJECT') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `secondary_status` enum('ACCEPT','PENDING','REJECT') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`user_apply_id`),
  UNIQUE KEY `UKrkb40kbx3053j826rjhnwcykd` (`apply_year`,`apply_semester`,`applier_id`),
  KEY `FK9fo16v3aeh6px0h2gd3b5s1ua` (`applier_id`),
  KEY `fk_user_apply_primary_study` (`primary_study`),
  KEY `fk_user_apply_secondary_study` (`secondary_study`),
  CONSTRAINT `FK9fo16v3aeh6px0h2gd3b5s1ua` FOREIGN KEY (`applier_id`) REFERENCES `tb_user` (`user_id`),
  CONSTRAINT `fk_user_apply_primary_study` FOREIGN KEY (`primary_study`) REFERENCES `tb_study` (`study_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_user_apply_secondary_study` FOREIGN KEY (`secondary_study`) REFERENCES `tb_study` (`study_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


SET FOREIGN_KEY_CHECKS = 1;
