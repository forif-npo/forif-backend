# 운영진 지원·명예의 전당 backend 계약

## 계약 메타데이터

| 항목          | 값                                                     |
| ------------- | ------------------------------------------------------ |
| 계약 ID       | `staff-hall-contract-r2`                               |
| 작성일·시간대 | 2026-10-11 / Asia/Seoul                                |
| backend 기준  | local `dev` / `6e1f75b`                                |
| frontend 기준 | local `dev` / `6fb6335`                                |
| 이전 초안     | `staff-hall-contract-r0-front-draft`                   |
| 합의 상태     | frontend 2단계 검토 완료 / backend 구현·공동 검증 대기 |

이 문서는 1단계 설계 계약이며 아직 구현된 API가 아니다. `r2`는 사용자가 지원서 개인정보의 DB 영구 보관을 확정한 리비전이며, 2단계 모집 endpoint와 상태 계약은 `r1`에서 바뀌지 않았다. 실제 API 구현·연동 전에는 기능 완료로 간주하지 않는다.

## 기존 구현과 신규 경계

| 요구 기능   | 기존 구현                                                                             | 재사용 방식                                     | 신규 코드가 필요한 이유                | 기존 영향                           |
| ----------- | ------------------------------------------------------------------------------------- | ----------------------------------------------- | -------------------------------------- | ----------------------------------- |
| 인증 회원   | `User`, JWT principal `Long userId`                                                   | 요청 body가 아닌 인증 principal 사용            | 없음                                   | 기존 로그인 계약 유지               |
| 정회원 판정 | `StudyUserRepository.existsByUserIdAndStudyYearSemester`, `SemesterService.getActive` | 현재 활동 학기의 수강 관계 존재를 서버에서 검사 | 지원 자격 서비스만 추가                | 회비·스터디 API 변경 없음           |
| 회장단      | `StaffAccountService.requirePresidentTeam/requirePresident`                           | 모집·권한 관리에 재사용                         | 없음                                   | 문자열 affiliation 의미 유지        |
| 운영진 후보 | `ForifTeam`, `StaffAccount(ADMIN)`                                                    | 회차 학기 명단과 ADMIN 계정의 교집합            | 회차별 reviewer grant 필요             | 전역 ADMIN을 새로 부여하지 않음     |
| 활동 학기   | `SemesterService`                                                                     | 생성 시 연도·학기 기본값과 검증에 사용          | 회차는 값 snapshot으로 보존            | 학기 전환이 과거 회차를 바꾸지 않음 |
| 모집 기간   | 스터디 `SemesterPhase`                                                                | 재사용하지 않음                                 | 운영진 모집은 독립 회차·기간·상태 필요 | 기존 스터디 일정 무영향             |
| 지원서      | `tb_user_apply`                                                                       | 재사용하지 않음                                 | 스터디 선택·합불 모델과 목적이 다름    | 기존 지원 API 무영향                |
| 응답·오류   | `ApiResponse`, `CursorPageResponse`, `ForifException`                                 | 그대로 사용                                     | 도메인 ErrorCode만 추가                | 전역 포맷 유지                      |
| 공개 이미지 | `FilePort`, 공개 `/api/v1/files/**`                                                   | 전시 이미지에 한해 재사용                       | 지원서 첨부는 비공개 경로가 필요       | 첨부 미채택 시 변경 없음            |

### 정회원의 정확한 판정

`정회원`은 지원 시점의 현재 활동 학기에 대해 아래가 참인 회원이다.

```text
authenticated_user exists
AND tb_study_user에
    user_id = authenticated_user_id
    AND 연결된 tb_study.act_year = active_semester.act_year
    AND 연결된 tb_study.act_semester = active_semester.act_semester
인 행이 1개 이상 존재
```

`tb_member_semester_check`의 회비 납부 플래그만으로 판정하지 않는다. 현재 구현에서 최종 활동부원 등록은 회비·구글폼 확인 뒤 `tb_study_user`에 생성되고, 등록 철회 시 해당 학기 관계가 삭제되기 때문이다. 회장단·멘토·ADMIN도 이 조건을 별도로 충족해야 지원할 수 있다.

## 확정 정책과 제안 정책

| 항목           | 계약 내용                                                    | 상태                         |
| -------------- | ------------------------------------------------------------ | ---------------------------- |
| 지원 자격      | 위 정회원 판정 + 유효한 USER 인증                            | 사용자 결정의 backend 구체화 |
| 지원 횟수      | 한 회차당 회원 1명당 지원서 1개                              | 확정                         |
| 지망           | 1지망 필수, 2지망 선택, 동일 부서 중복 금지                  | 사용자 결정의 backend 구체화 |
| 보관           | 지원서 개인정보와 답변·지망을 DB에 영구 보관, 자동 삭제 없음 | 확정                         |
| 활성 공개 회차 | 동시에 `PUBLISHED` 1개                                       | 제안, frontend 검토 필요     |
| 접수 경계      | `starts_at <= now < ends_at`; 미설정은 닫힘                  | 제안, frontend 검토 필요     |
| 제출 기능      | 1차는 즉시 제출만. 임시저장·수정·취소·재지원·첨부 없음       | 제안, 운영 승인 필요         |
| reviewer       | 기존 ADMIN + 회차 학기 운영진, 즉시 부여·회수                | 제안, 운영 승인 필요         |
| 이해충돌       | 같은 회차 지원자는 reviewer 부여 불가, 지원 후에도 열람 불가 | 제안, 운영 승인 필요         |
| 전시           | 수동 등록·미리보기·게시·게시 중단                            | 제안, 운영 승인 필요         |
| 전시 편집      | 회장단만                                                     | 제안, 운영 승인 필요         |

## 상태와 시간

### 모집 저장 상태

`DRAFT -> PUBLISHED -> CLOSED` 또는 `DRAFT|PUBLISHED -> CANCELED`만 허용한다. `CLOSED`와 `CANCELED`는 종단 상태다. 게시 중인 회차가 있으면 다른 회차 게시 요청은 409로 거절하며 서버가 기존 회차를 암묵적으로 닫지 않는다.

공개 응답의 `application_status`는 저장하지 않고 다음 순서로 계산한다.

1. `CANCELED`이면 `CANCELED`.
2. `CLOSED`이거나 `now >= ends_at`이면 `CLOSED`.
3. 미게시 또는 시작·종료가 없으면 공개하지 않거나 관리자 응답에서 `CLOSED`.
4. `now < starts_at`이면 `UPCOMING`.
5. 그 밖은 `OPEN`.

시간은 DB `DATETIME(6)`에 Asia/Seoul 현지 시각으로 저장하고 API에는 offset 포함 ISO 8601(`2026-10-11T10:00:00+09:00`)로 보낸다. 정확히 시작 시각에는 열리고 정확히 종료 시각에는 닫힌다. 자연 종료는 `ended_at=ends_at`, 조기 마감·취소는 action 처리 시각을 `ended_at`에 기록한다.

### 지원서

1차 상태는 `SUBMITTED` 하나뿐이다. 수정·취소가 승인되기 전까지 관련 상태와 endpoint를 만들지 않는다.

### 전시

`DRAFT -> PUBLISHED -> UNPUBLISHED`, `UNPUBLISHED -> PUBLISHED`를 허용한다. 최초 게시 때 `published_at`을 설정하고 재게시는 새 게시 시각으로 갱신한다.

## 권한 매트릭스

| 행위                          | public | 정회원 USER |   일반 ADMIN |       회장단 | 유효 reviewer |
| ----------------------------- | -----: | ----------: | -----------: | -----------: | ------------: |
| 공개 모집·게시 전시 조회      |      O |           O |            O |            O |             O |
| 지원 제출·본인 조회           |      X |           O | 정회원이면 O | 정회원이면 O |  정회원이면 O |
| 모집 생성·수정·게시·마감·취소 |      X |           X |            X |            O |             X |
| 지원서 목록·상세              |      X |      본인만 |            X |            O | 해당 회차만 O |
| reviewer 후보·부여·회수       |      X |           X |            X |            O |             X |
| 전시 초안·미리보기·편집·게시  |      X |           X |            X |      O(제안) |             X |

reviewer 권한은 매 요청마다 DB의 회차 grant가 활성인지 검사한다. JWT에 넣거나 전역 ADMIN 권한을 확대하지 않는다. 회수·만료 후 다음 요청부터 403이다.

## 최소 데이터 모델과 DB 제약

다음 migration 번호는 현재 최고 버전 V4를 확인한 뒤 구현 시점에 다시 충돌 여부를 확인해 정한다. 1단계에서는 SQL을 만들거나 DB에 적용하지 않는다.

| 저장 단위                         | 주요 필드                                                                                       | 핵심 제약·인덱스                                                                                    |
| --------------------------------- | ----------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| `tb_staff_recruitment`            | 학기 snapshot, 제목/안내, 기간, status, ended_at, close_reason, version                         | 기간 CHECK, status+기간 조회 index, `@Version`                                                      |
| `tb_staff_recruitment_department` | recruitment FK, name, description, order, active                                                | `(recruitment_id,name)` unique, `(recruitment_id,display_order)` index                              |
| `tb_staff_recruitment_question`   | recruitment FK, label, type, required, max_length, order                                        | 타입/길이 CHECK, 회차+순서 index                                                                    |
| `tb_staff_application`            | recruitment FK, nullable applicant user FK, 이름/이메일/전화/학과 snapshot, submitted_at        | `(recruitment_id,applicant_user_id)` unique, 제출 목록 index                                        |
| `tb_staff_application_choice`     | application FK, department FK, priority                                                         | `(application_id,priority)` PK/unique, `(application_id,department_id)` unique, priority 1..2 CHECK |
| `tb_staff_application_answer`     | application FK, question ID와 label/type snapshot, answer                                       | `(application_id,question_id)` unique                                                               |
| `tb_staff_recruitment_reviewer`   | recruitment/user/granter, granted/revoked/expires timestamps                                    | `(recruitment_id,user_id)` unique, 활성 조회 index                                                  |
| `tb_staff_recruitment_audit`      | recruitment, application 식별값, actor, action, occurred_at                                     | 개인정보 본문 미저장, action+time index                                                             |
| `tb_hall_of_fame_entry`           | nullable user FK, 이름 snapshot, 학기, slug, 소개, image key, badges JSON, status/order/version | slug unique, 공개 시즌+순서 index                                                                   |
| `tb_hall_of_fame_achievement`     | entry FK, 제목/설명/link/order                                                                  | entry+order index                                                                                   |

`application`의 회원 FK는 `ON DELETE SET NULL`로 두고 제출 당시 최소 개인정보 snapshot을 영구 보관한다. 자식 choice/answer도 application과 함께 영구 보관하며 application 삭제 API와 자동 삭제 작업을 두지 않는다. FK cascade는 예외적인 DB 무결성 보호용으로만 정의한다. 모집·부서·문항은 제출이 생긴 뒤 물리 삭제하지 않는다. 전시 회원 FK도 `ON DELETE SET NULL`로 두어 공개 승인을 받은 snapshot은 회원 탈퇴와 별도 철회 절차로 관리한다.

MySQL에는 `PUBLISHED` 회차가 하나만 존재하도록 nullable 단일 슬롯 또는 동등한 DB 유일 제약을 둔다. 구체 DDL은 V5 번호를 예약하지 않고 구현 직전 현재 migration 목록과 MySQL 8 동작을 재확인한다.

## 개인정보 영구 보관

- 지원서 aggregate(application, 제출 당시 개인정보 snapshot, 답변, 지망)는 모집 종료·조기 마감·취소와 관계없이 DB에 영구 보관한다.
- 기간 기반 만료 시각과 자동 삭제 scheduler/batch는 구현하지 않는다.
- 회원 탈퇴 시 회원 FK는 `ON DELETE SET NULL`로 분리하되 제출 snapshot·답변·지망은 유지한다.
- 향후 법적·운영상 삭제 요구를 도입하려면 삭제 권한, 감사, backup 처리와 복구 불가 범위를 별도 계약 리비전으로 먼저 확정한다.
- audit에는 답변·연락처·이름을 기록하지 않고 application 숫자 식별자와 reviewer actor/action/time만 기록한다.
- 개인 응답은 항상 `Cache-Control: no-store, private`; URL·서버 log에 답변이나 연락처를 넣지 않는다.

## 공통 API 규약

- body와 응답은 기존 Jackson `SNAKE_CASE`, query parameter는 표에 적힌 snake_case 이름을 명시한다.
- 모든 JSON은 기존 `ApiResponse<T>`로 감싼다.
- 목록은 `CursorPageResponse<T>`를 사용한다. `page`가 있으면 offset(0부터), 없으면 cursor이며 기본 `size=20`, 최대 100이다.
- 목록 기본 정렬은 안정적인 `id DESC`; 전시는 `display_order ASC, id DESC`다.
- public 모집 JSON은 `Cache-Control: no-cache`; 공개 전시는 `public, max-age=60`; 초안·본인·관리 API는 `no-store, private`다.
- 게시 중단은 60초 내 public cache에 반영한다. 더 강한 즉시 철회가 필요하면 구현 전에 cache invalidation 계약을 추가한다.

## endpoint 계약

### 공개·지원자 API

| method·path                                                       | 인증        | 요청                                | 응답·특이사항                            |
| ----------------------------------------------------------------- | ----------- | ----------------------------------- | ---------------------------------------- | ----------- |
| `GET /api/v1/staff-recruitments/current`                          | public      | 없음                                | 공개 회차 또는 `data:null`; draft 미노출 |
| `GET /api/v1/staff-recruitments/{recruitment_id}`                 | public      | path                                | 공개 회차 상세, 아니면 404               |
| `POST /api/v1/staff-recruitments/{recruitment_id}/applications`   | USER+정회원 | choices, answers                    | 201, 중복 409, 기간/자격 검증            |
| `GET /api/v1/staff-recruitments/{recruitment_id}/applications/me` | USER        | 없음                                | 본인 제출 상세, 없으면 404               |
| `GET /api/v1/hall-of-fame`                                        | public      | `act_year`, `act_semester`, `cursor | page`, `size`                            | 게시 항목만 |
| `GET /api/v1/hall-of-fame/{slug}`                                 | public      | slug                                | 게시 상세만, 초안·중단은 404             |

현재 모집 응답의 핵심 예시는 다음과 같다.

```json
{
  "timestamp": 1791680400000,
  "data": {
    "recruitment_id": 12,
    "title": "2027-1 운영진 모집",
    "summary": "FORIF를 함께 운영할 분을 모집합니다.",
    "description": "모집 안내",
    "act_year": 2027,
    "act_semester": 1,
    "semester_label": "27-1",
    "starts_at": "2026-12-01T00:00:00+09:00",
    "ends_at": "2026-12-15T18:00:00+09:00",
    "publication_status": "PUBLISHED",
    "application_status": "OPEN",
    "application_open": true,
    "application_unavailable_reason": null,
    "departments": [
      {
        "department_id": 31,
        "name": "기획팀",
        "description": null,
        "display_order": 1
      }
    ],
    "questions": [
      {
        "question_id": 41,
        "label": "지원 동기",
        "description": null,
        "type": "LONG_TEXT",
        "required": true,
        "max_length": 1000,
        "display_order": 1
      }
    ],
    "updated_at": "2026-10-11T15:00:00+09:00"
  },
  "error_code": null,
  "message": "Success"
}
```

제출 요청과 성공 응답은 다음과 같다. 2지망은 생략할 수 있다.

```json
{
  "department_choices": [
    { "department_id": 31, "priority": 1 },
    { "department_id": 32, "priority": 2 }
  ],
  "answers": [{ "question_id": 41, "answer": "지원 동기입니다." }]
}
```

```json
{
  "timestamp": 1791680400000,
  "data": {
    "application_id": 101,
    "recruitment_id": 12,
    "status": "SUBMITTED",
    "submitted_at": "2026-12-01T10:00:00+09:00"
  },
  "error_code": null,
  "message": "Success"
}
```

서버는 질문이 해당 회차 소속인지, 필수 답변 존재와 `max_length`, 부서 활성 여부, 1지망 존재, 우선순위·부서 중복을 한 트랜잭션에서 검증한다. unique 제약으로 동시 중복 제출도 막는다.

### 모집·지원서 관리 API

| method·path                                                               | 권한            | 요청·filter                                   | 응답                        |
| ------------------------------------------------------------------------- | --------------- | --------------------------------------------- | --------------------------- |
| `GET /api/v1/admin/staff-recruitments`                                    | 회장단          | status, cursor/page/size, sort                | 초안 포함 목록              |
| `POST /api/v1/admin/staff-recruitments`                                   | 회장단          | 회차+부서+문항                                | 201 DRAFT                   |
| `GET /api/v1/admin/staff-recruitments/{id}`                               | 회장단          | 없음                                          | 전체 관리 상세              |
| `PATCH /api/v1/admin/staff-recruitments/{id}`                             | 회장단          | 변경 필드+version                             | 갱신 상세, stale 409        |
| `POST /api/v1/admin/staff-recruitments/{id}/publish`                      | 회장단          | version                                       | PUBLISHED, 활성 충돌 409    |
| `POST /api/v1/admin/staff-recruitments/{id}/close`                        | 회장단          | reason, version                               | CLOSED+ended_at             |
| `POST /api/v1/admin/staff-recruitments/{id}/cancel`                       | 회장단          | reason, version                               | CANCELED+ended_at           |
| `GET /api/v1/admin/staff-recruitments/{id}/applications`                  | 회장단/reviewer | department_id, search, cursor/page/size, sort | 이름·지망·제출 시각만       |
| `GET /api/v1/admin/staff-recruitments/{id}/applications/{application_id}` | 회장단/reviewer | 없음                                          | snapshot 연락처와 전체 답변 |

지원서 목록의 `search`는 제출 snapshot 이름만 대상으로 하고 전화·이메일 검색은 제공하지 않는다. `department_id`는 1·2지망 모두 일치한다. sort 허용값은 `applicationId`, `applicantName`, `submittedAt`; 기본은 `submittedAt:DESC, applicationId:DESC`다.

지원서 상세는 다음 모양이다.

```json
{
  "application_id": 101,
  "recruitment_id": 12,
  "applicant_user_id": 2020123456,
  "applicant_name": "홍길동",
  "applicant_department": "컴퓨터소프트웨어학부",
  "applicant_email": "masked-in-example@hanyang.ac.kr",
  "applicant_phone_num": "01000000000",
  "applied_departments": [
    { "department_id": 31, "name": "기획팀", "priority": 1 }
  ],
  "status": "SUBMITTED",
  "submitted_at": "2026-12-01T10:00:00+09:00",
  "answers": [
    {
      "question_id": 41,
      "question_label": "지원 동기",
      "question_type": "LONG_TEXT",
      "answer": "예시 답변"
    }
  ]
}
```

### reviewer API

| method·path                                                        | 권한   | 동작                                             |
| ------------------------------------------------------------------ | ------ | ------------------------------------------------ |
| `GET /api/v1/admin/staff-recruitments/{id}/reviewer-candidates`    | 회장단 | 회차 학기의 `ForifTeam` + 현재 ADMIN 계정 교집합 |
| `GET /api/v1/admin/staff-recruitments/{id}/reviewers`              | 회장단 | active/revoked/expired 포함                      |
| `POST /api/v1/admin/staff-recruitments/{id}/reviewers`             | 회장단 | `{user_id, expires_at}` 즉시 부여                |
| `DELETE /api/v1/admin/staff-recruitments/{id}/reviewers/{user_id}` | 회장단 | idempotent 회수, 204                             |

후보라는 사실만으로 열람할 수 없으며 active grant가 필요하다. 만료 기본값은 명시하지 않고 요청 필수로 둔다. 접수 종료와 열람 만료는 별개다.

### 전시 관리 API

| method·path                                            | 권한(제안) | 동작                             |
| ------------------------------------------------------ | ---------- | -------------------------------- |
| `GET /api/v1/admin/hall-of-fame`                       | 회장단     | 모든 상태 목록                   |
| `POST /api/v1/admin/hall-of-fame`                      | 회장단     | DRAFT 생성                       |
| `GET /api/v1/admin/hall-of-fame/{entry_id}`            | 회장단     | 초안 포함 상세/미리보기 데이터   |
| `PATCH /api/v1/admin/hall-of-fame/{entry_id}`          | 회장단     | version 기반 수정                |
| `POST /api/v1/admin/hall-of-fame/{entry_id}/image`     | 회장단     | 공개 이미지 저장, image_url 반환 |
| `POST /api/v1/admin/hall-of-fame/{entry_id}/publish`   | 회장단     | PUBLISHED                        |
| `POST /api/v1/admin/hall-of-fame/{entry_id}/unpublish` | 회장단     | UNPUBLISHED                      |

slug는 영문 소문자·숫자·하이픈 3~80자, 앞뒤 하이픈 금지다. 외부 링크는 `https`만 허용한다. badges는 최대 10개, 각 30자; achievement는 최대 20개로 제한하는 안을 frontend가 검토한다.

## 오류 계약

번호는 현재 `ErrorCode` 최고 번호 147 뒤를 사용한 1단계 예약안이다. 구현 시 다른 PR과 충돌하면 계약 리비전을 올려 함께 변경한다.

| enum / code                                             | HTTP | 조건                               |
| ------------------------------------------------------- | ---: | ---------------------------------- |
| `STAFF_RECRUITMENT_NOT_FOUND` / `FOR148-404`            |  404 | 회차 없음 또는 공개 불가           |
| `STAFF_RECRUITMENT_INVALID_PERIOD` / `FOR149-400`       |  400 | 일정 없음·역전                     |
| `STAFF_RECRUITMENT_INVALID_STATUS` / `FOR150-400`       |  400 | 허용되지 않은 전이                 |
| `STAFF_RECRUITMENT_ACTIVE_CONFLICT` / `FOR151-409`      |  409 | 다른 게시 회차 존재                |
| `STAFF_RECRUITMENT_NOT_OPEN` / `FOR152-400`             |  400 | 시작 전·마감·취소·미설정           |
| `STAFF_APPLICATION_ELIGIBILITY_REQUIRED` / `FOR153-403` |  403 | 현재 활동 학기 정회원 아님         |
| `STAFF_APPLICATION_ALREADY_EXISTS` / `FOR154-409`       |  409 | 회차당 중복 제출                   |
| `STAFF_APPLICATION_NOT_FOUND` / `FOR155-404`            |  404 | 본인/관리 지원서 없음              |
| `STAFF_APPLICATION_INVALID_CHOICE` / `FOR156-400`       |  400 | 1지망 누락·중복·타 회차 부서       |
| `STAFF_APPLICATION_INVALID_ANSWER` / `FOR157-400`       |  400 | 질문 소속·필수·길이 오류           |
| `STAFF_REVIEWER_NOT_ALLOWED` / `FOR158-403`             |  403 | 해당 회차 열람 권한 없음/회수/만료 |
| `STAFF_REVIEWER_ALREADY_EXISTS` / `FOR159-409`          |  409 | grant 중복                         |
| `STAFF_REVIEWER_CONFLICT` / `FOR160-409`                |  409 | 지원자 이해충돌 등                 |
| `HALL_OF_FAME_ENTRY_NOT_FOUND` / `FOR161-404`           |  404 | 항목 없음 또는 비게시 공개 접근    |
| `HALL_OF_FAME_SLUG_ALREADY_EXISTS` / `FOR162-409`       |  409 | slug 중복                          |
| `HALL_OF_FAME_INVALID_STATUS` / `FOR163-400`            |  400 | 잘못된 게시 전이                   |

인증 오류는 기존 `FOR017-401`~`FOR019-401`, 일반 권한은 `FOR020-403`, validation은 `FOR003-400`, optimistic lock은 도메인별 409, 최종 DB 경합은 `FOR145-409`를 재사용한다.

오류 예시는 다음과 같다.

```json
{
  "timestamp": 1791680400000,
  "data": null,
  "error_code": "FOR154-409",
  "message": "이미 이 모집 회차에 지원했습니다."
}
```

## 호환·migration·복구 계획

- 신규 경로와 신규 테이블만 추가하므로 기존 frontend/API/DB 행을 변경하지 않는다.
- migration은 additive하게 작성하고 V1~V4를 수정하지 않는다.
- 배포 순서는 migration+backend 먼저, 공개 읽기·인가 검증 후 frontend다.
- 구버전 JAR은 신규 테이블을 무시하므로 schema 자체는 호환되지만, 신버전으로 받은 지원서를 구버전에서 처리할 수 없다.
- rollout 실패 시 JAR만 구버전으로 되돌릴 수 있다. DB 테이블 drop은 하지 않고 forward fix하며, 실제 개인정보가 생긴 뒤 rollback DDL을 자동 실행하지 않는다.
- 빈 MySQL에는 `FlywayMigrationSchemaTest`, 기존 데이터 복제본에는 migration rehearsal이 필요하다.

## 합의가 필요한 항목

아래는 backend가 임의 확정하지 않은 출시 차단 정책이다.

1. 임시저장·제출 후 수정·취소·재지원·첨부를 1차에서 제외할지.
2. 같은 회차 지원자의 reviewer 부여를 금지할지와 기존 reviewer가 나중에 지원할 때의 처리.
3. 전시 선정 기준, 당사자 공개 동의·철회 절차, 일반 ADMIN 편집 허용 여부.
4. public 전시 게시 중단을 60초 cache 지연으로 허용할지.

2단계 모집 설정은 위 항목과 독립적으로 진행할 수 있다.
