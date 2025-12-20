-- 1. User 5 (이서준) : 2025-09-27 09:30 신청
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (5, 2025, 2, 1, 'Forif 웹 개발 스터디', '웹 개발의 기초부터 심화까지 배우고 싶습니다.', 3, 'Python 프로그래밍 기초', '파이썬 데이터 분석에 관심이 있어 지원합니다.', 0, 'PENDING', 'PENDING', '2025-09-27 09:30:00', '2025-09-27 09:30:00');

-- 2. User 12 (이준혁) : 2025-09-27 10:15 신청 -> 09-28 승낙
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (12, 2025, 2, 4, 'React Native 모바일 앱 개발', '크로스 플랫폼 앱 개발을 경험해보고 싶어 지원했습니다.', NULL, NULL, NULL, 1, 'ACCEPT', NULL, '2025-09-27 10:15:00', '2025-09-28 10:00:00');

-- 4. User 45 (양민아) : 2025-09-28 11:00 신청
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (45, 2025, 2, 21, 'TensorFlow 컴퓨터 비전 프로젝트', 'CV 분야 프로젝트 경험을 쌓고 싶습니다.', 3, 'Python 프로그래밍 기초', '기초 문법을 다시 탄탄히 하고 싶습니다.', 0, 'PENDING', 'PENDING', '2025-09-28 11:00:00', '2025-09-28 11:00:00');

-- 5. User 60 (염준호) : 2025-09-28 16:45 신청 -> 09-30 1지망 승낙
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (60, 2025, 2, 22, 'TypeScript 실무 활용', 'JS의 한계를 느껴 타입스크립트를 도입하고 싶습니다.', 42, 'Figma UI/UX 디자인 기초', '개발자지만 디자인 툴 사용법을 익히고 싶습니다.', 1, 'ACCEPT', 'PENDING', '2025-09-28 16:45:00', '2025-09-30 14:00:00');

-- 6. User 75 (호재원) : 2025-09-29 10:30 신청
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (75, 2025, 2, 81, 'Quantum Computing 입문', '양자 역학과 컴퓨터 공학의 접점에 흥미가 있습니다.', NULL, NULL, NULL, 0, 'PENDING', NULL, '2025-09-29 10:30:00', '2025-09-29 10:30:00');

-- 7. User 90 (신다은) : 2025-09-29 13:15 신청
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (90, 2025, 2, 82, 'Astro 정적 사이트 생성', '콘텐츠 중심의 웹사이트 최적화에 관심이 많습니다.', 62, 'Vite 모던 빌드 도구', '빠른 빌드 환경 구성을 배우고 싶습니다.', 0, 'PENDING', 'PENDING', '2025-09-29 13:15:00', '2025-09-29 13:15:00');

-- 9. User 120 (김예빈) : 2025-09-30 15:20 신청 -> 10-01 처리
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (120, 2025, 2, 1, 'Forif 웹 개발 스터디', '동아리 대표 스터디라 꼭 참여하고 싶습니다.', 4, 'React Native 모바일 앱 개발', '앱 배포 경험이 필요합니다.', 1, 'ACCEPT', 'REJECT', '2025-09-30 15:20:00', '2025-10-01 11:00:00');

-- 10. User 135 (팽민호) : 2025-10-01 10:00 신청
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (135, 2025, 2, 3, 'Python 프로그래밍 기초', '비전공자로서 프로그래밍에 입문하고 싶습니다.', NULL, NULL, NULL, 0, 'PENDING', NULL, '2025-10-01 10:00:00', '2025-10-01 10:00:00');

--------------------------- 페이징 테스트를 위한 지원 데이터 ---------------------------

-- 1. User 50: 1지망 Study 2, 2지망 Study 3 (2025-10-02 10:00)
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (50, 2025, 2, 2, 'Spring API 서버 개발 스터디', '백엔드 기초를 다지고 싶습니다.', 3, 'Python 프로그래밍 기초', '데이터 분석도 관심있습니다.', 0, 'PENDING', 'PENDING', '2025-10-02 10:00:00', '2025-10-02 10:00:00');

-- 2. User 51: 1지망 Study 3, 2지망 Study 2 (2025-10-02 11:00) -> 2지망으로 Study 2 지원
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (51, 2025, 2, 3, 'Python 프로그래밍 기초', '파이썬이 1순위입니다.', 2, 'Spring API 서버 개발 스터디', '자바도 배워보고 싶습니다.', 0, 'ACCEPT', 'PENDING', '2025-10-02 11:00:00', '2025-10-02 11:00:00');

-- 3. User 52: 1지망 Study 2, 2지망 Study 4 (2025-10-02 12:00)
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (52, 2025, 2, 2, 'Spring API 서버 개발 스터디', '스프링부트 프로젝트 경험이 필요합니다.', 4, 'React Native 모바일 앱 개발', '앱 개발도 궁금합니다.', 0, 'PENDING', 'PENDING', '2025-10-02 12:00:00', '2025-10-02 12:00:00');

-- 4. User 53: 1지망 Study 2, 2지망 없음 (2025-10-02 13:00)
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (53, 2025, 2, 2, 'Spring API 서버 개발 스터디', '스프링만 팝니다.', NULL, NULL, NULL, 0, 'PENDING', NULL, '2025-10-02 13:00:00', '2025-10-02 13:00:00');

-- 5. User 54: 1지망 Study 5, 2지망 Study 2 (2025-10-02 14:00) -> 2지망으로 Study 2 지원
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (54, 2025, 2, 5, 'JavaScript ES6+ 마스터', '프론트엔드 지망입니다.', 2, 'Spring API 서버 개발 스터디', '백엔드 협업을 위해 신청합니다.', 0, 'REJECT', 'ACCEPT', '2025-10-02 14:00:00', '2025-10-03 09:00:00');

-- 6. User 55: 1지망 Study 2, 2지망 Study 6 (2025-10-02 15:00)
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (55, 2025, 2, 2, 'Spring API 서버 개발 스터디', 'API 설계 능력을 키우고 싶습니다.', 6, '머신러닝과 딥러닝 실전', 'AI 모델 서빙에 관심있습니다.', 0, 'ACCEPT', 'PENDING', '2025-10-02 15:00:00', '2025-10-02 15:00:00');

-- 7. User 56: 1지망 Study 2, 2지망 Study 7 (2025-10-02 16:00)
INSERT INTO tb_user_apply (applier_id, apply_year, apply_semester, primary_study, primary_study_name, primary_intro, secondary_study, secondary_study_name, secondary_intro, pay_status, primary_status, secondary_status, created_at, updated_at)
VALUES (56, 2025, 2, 2, 'Spring API 서버 개발 스터디', '자바 웹 개발자가 꿈입니다.', 7, 'Node.js 백엔드 개발', '다른 백엔드 기술도 궁금합니다.', 0, 'PENDING', 'PENDING', '2025-10-02 16:00:00', '2025-10-02 16:00:00');