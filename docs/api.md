# API

컨트롤러 22개, 엔드포인트 155개입니다. 여기서는 규약과 리소스별 개요만 다룹니다. **파라미터·스키마의 정본은 Swagger 입니다.**

- [Swagger UI](https://dev.forif.org/swagger-ui/index.html)
- Scalar: `/scalar`
- OpenAPI JSON: `/v3/api-docs`

## 규약

응답 래퍼, 예외, 직렬화, 페이지네이션 규약은 [architecture.md](architecture.md#공통-규약) 에 정리돼 있습니다. 요약하면 이렇습니다.

- 모든 응답은 `ApiResponse<T>` 로 감쌉니다 (파일 서빙 제외).
- 바디는 `SNAKE_CASE`, 쿼리 파라미터는 아닙니다.
- 목록은 `CursorPageResponse<T>` 이며 `cursor`·`page` 를 모두 생략하면 커서 모드 첫 페이지입니다.
- 인증은 `Authorization: Bearer`, 갱신은 `refreshToken` 쿠키입니다.
- 에러는 `FOR{번호}-{HTTP상태}` 형식이며 `GET /api/v1/error-codes` 가 전체 목록을 반환합니다.

## 업로드

| 항목 | 값 |
|---|---|
| 파일 하나 | 최대 5MB |
| 요청 전체 | 최대 20MB |

초과하면 `INVALID_FILE_ATTACHMENT`(400) 입니다. 파일이 필요한 엔드포인트는 `consumes = multipart/form-data` 를 명시합니다. 일부 경로(`PATCH /api/v1/admin/studies/{studyId}`, `POST /api/v1/products/applications`)는 같은 경로에 JSON 용과 multipart 용 핸들러를 `consumes` 로 분기해 둘 다 제공합니다.

## 리소스별 개요

### 부원 · 인증

| 경로 | 컨트롤러 | 개수 |
|---|---|---|
| `/api/v1/users` | `UserController` | 13 |

Google OAuth 기반 회원가입·로그인·토큰 재발급·로그아웃과 마이페이지(내 수강 스터디, 신청서, 수료증, 내 정보·설정, 기본정보·전화번호 수정, 타 부원 조회)를 담당합니다.

회원가입은 `department_id` 를 받습니다. 전환 기간 동안 학과명 문자열(`department`)도 허용합니다.

### 스터디

| 경로 | 컨트롤러 | 개수 |
|---|---|---|
| `/api/v1/studies`, `/api/v1/admin/studies` | `StudyController` | 10 |
| `/api/v1/study-apply` | `StudyApplyController` | 6 |
| `/api/v1/studies/{studyId}/attendances` | `StudyAttendanceController` | 2 |
| `/api/v1/admin/...` | `AdminCertificateController` | 8 |
| `/api/v1/studies/...` | `MentorConfirmationController` | 1 |

스터디 목록·상세 조회는 공개입니다. 어드민 경로는 수정·삭제·승인·반려와 자율부원 스터디 생성을 담당합니다.

`StudyApplyController` 는 멘토의 개설 신청·수정·재신청·취소와 내 신청 조회입니다. `AdminCertificateController` 는 멘토확인서·수료증 발급 대상 조회, 발급, 수동 발급, 회장 서명 등록입니다.

### 수강 신청

| 경로 | 컨트롤러 | 개수 |
|---|---|---|
| `/api/v1/users/apply` | `UserApplyController` | 9 |
| `/api/v1/admin/study-applications` | `AdminStudyApplicationController` | 3 |

멘티의 신청·상태 조회·수정·취소와 멘토의 신청자 목록·상세 조회·합불 처리입니다. 어드민 쪽은 현재 학기 신청 목록 조회와 자율부원 신청 일괄 처리이며 클래스 단위로 `hasRole('ADMIN')` 입니다.

### 해커톤

| 경로 | 컨트롤러 | 개수 |
|---|---|---|
| `/api/v1/hackathons`, `/api/v1/admin/...`, `/api/v1/archive/...` | `HackathonController` | 41 |

해커톤 CRUD·상태 변경, 참가 등록·취소, 팀 생성·수정·해산·가입 신청·승인, 결과물 제출·조회, 평가 기준 CRUD 와 점수 제출·집계, 수상 결과 CRUD, 종료된 해커톤 아카이브 조회를 모두 담습니다.

단일 클래스가 41개 매핑을 갖고 있어 도메인별 분리를 검토할 만합니다.

### 운영진

| 경로 | 컨트롤러 | 개수 |
|---|---|---|
| `/api/v1/staff`, `/api/v1/president`, `/api/v1/admin/mentors`, `/api/v1/admin/users` | `StaffAccountController` | 15 |
| `/api/v1/forif-team`, `/api/v1/admin/forif-team` | `ForifTeamController` | 6 |

`StaffAccountController` 는 세 갈래를 한 클래스에서 다룹니다 — 운영진 로그인·내 정보·비밀번호 변경(`/staff`), 계정 CRUD 와 회장 위임(`/president`), 멘토·부원 목록 조회와 부원 수정·삭제(`/admin`).

`/president` 계열은 `@PreAuthorize` 로는 `hasRole('ADMIN')` 만 걸고, 회장단 여부는 서비스에서 `requirePresidentTeam` 으로 검사합니다.

### 학기

| 경로 | 컨트롤러 | 개수 |
|---|---|---|
| `/api/v1/semesters` | `SemesterController` | 2 |
| `/api/v1/semester-schedules`, `/api/v1/admin/semester-schedules` | `SemesterScheduleController` | 3 |
| `/api/v1/admin/semesters` | `AdminSemesterController` | 2 |

현재 학기와 일정 조회는 공개입니다. 일정 저장(`PUT /api/v1/admin/semester-schedules/{year}/{semester}`)은 **전체 교체**입니다 — 요청에 없는 단계는 삭제됩니다. 회장단만 호출할 수 있습니다.

학기 전환(`/admin/semesters`)은 회장 전용입니다.

### 회비

| 경로 | 컨트롤러 | 개수 |
|---|---|---|
| `/api/v1/admin/dues` | `DuesController` | 3 |

회비·구글폼 확인 상태 목록 조회, 일괄 저장, 등록 철회입니다. 목록은 `dues_paid` · `google_form_submitted` 로 필터할 수 있고 **상단 요약 통계는 필터와 무관하게 전체 합격자 기준**입니다.

### 알림

| 경로 | 컨트롤러 | 개수 |
|---|---|---|
| `/api/v1/notifications` | `NotificationController` | 4 |

수신자 목록 조회(`target_type` 으로 대상 선택), 알림톡 발송, 템플릿 조회, 발송 이력 조회입니다. 대상 종류는 [domain.md](domain.md#알림) 를 보세요.

발송 이력은 Solapi 에 보관된 최근 6개월 알림톡을 Solapi 커서 순서로 조회합니다.

### 그 외

| 경로 | 컨트롤러 | 개수 | 내용 |
|---|---|---|---|
| `/api/v1/posts` | `PostController` | 9 | 공지사항·FAQ CRUD (조회는 공개) |
| `/api/v1/products` | `ProductController` | 7 | 부원 서비스 쇼케이스 조회·등록 신청 |
| `/api/v1/admin/products` | `AdminProductController` | 8 | 승인·반려·운영상태·썸네일·삭제 |
| `/api/v1/departments` | `DepartmentController` | 1 | 단과대학·학과 목록 (공개) |
| `/api/v1/files/**` | `FileController` | 1 | 로컬 파일 서빙 |
| `/api/v1/error-codes` | `ErrorCodeController` | 1 | 전체 에러 코드 목록 |

`FileController` 는 `app.file.storage=local` 일 때만 등록됩니다(`@ConditionalOnProperty`). `download=true` 를 붙이면 `Content-Disposition: attachment` 로 내려줍니다.

## 새 엔드포인트를 추가할 때

- 응답은 `ApiResponse` 로 감싸고, 예외는 `ForifException(ErrorCode)` 로 던집니다.
- 목록이면 `CursorPageResponse` 를 쓰고 정렬은 `SortCriteria` 화이트리스트를 거칩니다.
- 요청 DTO 에 `@Size` 등 검증을 걸고 컨트롤러에 `@Valid` 를 붙입니다. **DB 컬럼 길이와 맞추세요.** 안 그러면 MySQL 이 거부하고 `DATA_INTEGRITY_VIOLATION`(409)이라는 엉뚱한 응답이 나갑니다.
- 공개 조회면 `SecurityConfig` 의 **`GET` 블록**에 경로를 추가합니다.
- `@Tag` 와 `@Operation` 을 한국어로 답니다.
