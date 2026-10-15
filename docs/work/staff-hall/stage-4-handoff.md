# 4단계 frontend 인계 — 한지석

```text
계약 ID: staff-hall-contract-r2 (4단계 endpoint 구현 + 아래 결정 사항 반영)
backend 기준: feature/staff-recruitment-reviewer (feature/staff-recruitment-application 위에 쌓음)
frontend 기준: dev / 1d716a6 (4단계 UI 커밋됨)
연동 상태: backend 구현 완료 / 실제 서버 연동 검증 대기
```

## 요청한 4개 endpoint

경로와 응답 모양을 frontend adapter에 맞췄다. 후보·열람자 목록은 **페이지네이션 없이 배열**이다.

| method·path | 권한 | 응답 |
| --- | --- | --- |
| `GET /api/v1/admin/staff-recruitments/{id}/reviewer-candidates` | 회장단 | `CandidateResponse[]` |
| `GET /api/v1/admin/staff-recruitments/{id}/reviewers` | 회장단 | `ReviewerResponse[]` (회수·만료 포함) |
| `POST /api/v1/admin/staff-recruitments/{id}/reviewers` | 회장단 | `ReviewerResponse` |
| `DELETE /api/v1/admin/staff-recruitments/{id}/reviewers/{userId}` | 회장단 | 204, 본문 없음 |

추가로 요구사항 6번(열람 가능한 회차 찾기)에 대해 endpoint를 하나 더 만들었다.

| method·path | 권한 | 응답 |
| --- | --- | --- |
| `GET /api/v1/admin/staff-recruitments/reviewable` | ADMIN | `AdminStaffRecruitmentResponse[]` |

회장단은 전체 회차(최대 100건)를, 일반 열람자는 **유효한 권한이 있는 회차만** 받는다. 기존 관리자 목록(`GET /admin/staff-recruitments`)은 회장단 전용 그대로 두었다.

## DTO — frontend 임시 DTO와 일치

```json
// candidate
{
  "user_id": 2020111111,
  "user_name": "김운영",
  "department": "컴퓨터소프트웨어학부",
  "affiliation": "개발팀",
  "has_admin_account": true,
  "reviewer_status": null
}

// reviewer
{
  "user_id": 2020111111,
  "user_name": "김운영",
  "department": "컴퓨터소프트웨어학부",
  "affiliation": "개발팀",
  "status": "ACTIVE",
  "granted_at": "2026-10-15T14:00:00+09:00",
  "expires_at": "2026-12-31T23:59:59+09:00",
  "revoked_at": null
}

// grant request
{ "user_id": 2020111111, "expires_at": "2026-12-31T14:59:59.000Z" }
```

- `reviewer_status`는 한 번도 부여한 적 없으면 `null`, 그 외에는 `ACTIVE | REVOKED | EXPIRED`다.
- `status`는 저장값이 아니라 조회 시각으로 계산한다. 만료 시각이 지나면 조회만 해도 `EXPIRED`로 보인다.
- `expires_at`은 **필수**다. `toISOString()`이 만드는 UTC(`Z`) 형식도 그대로 받는다. 과거 시각이면 `FOR002-400`이다.
- 시각 응답은 2·3단계와 같이 offset 포함 ISO 8601이다.

## 결정 사항 — frontend 요구와 다르게 구현한 부분

1. **후보 범위**: 인계문 2번은 "운영진 ∩ ADMIN 계정"이었지만, frontend DTO에 `has_admin_account`가 있어 **회차 학기 운영진 전원**을 내려주고 플래그로 구분한다. 계정이 없는 사람에게 부여를 시도하면 `FOR160-409`로 거절한다. 화면에서 이유를 보여줄 수 있어 이 쪽이 낫다고 판단했다. 교집합만 원하면 알려 달라.
2. **`affiliation` 출처**: 후보 목록은 운영진 명단(`tb_forif_team.club_department`), 열람자 목록은 ADMIN 계정(`tb_staff_account.affiliation`)에서 가져온다. 두 값이 다르게 설정돼 있으면 화면에서 다르게 보일 수 있다. 하나로 통일할지 알려 달라.
3. **이해충돌**: 같은 회차 지원자에게는 부여할 수 없고(`FOR160`), 부여 후에 지원하면 그 시점부터 열람이 막힌다(`FOR158`). **회장단은 지원해도 차단하지 않는다.** 회장단까지 막으면 모집 운영 자체가 막힐 수 있어 보류했다. 운영 정책으로 확정이 필요하다.
4. **재부여**: 회수·만료된 권한을 다시 부여하면 같은 행이 되살아나고 `granted_at`·`expires_at`이 갱신된다. 이력 행이 중복으로 쌓이지 않는다.

## 권한 검사 동작

- 지원서 목록·상세는 **매 요청마다** 회차 권한을 다시 확인한다. JWT에는 아무것도 넣지 않는다.
- 회수 직후·만료 직후 다음 요청은 `FOR158-403`이다. 캐시하지 않으므로 별도 무효화가 필요 없다.
- 열람자는 모집 설정 변경·다른 사람 초대를 할 수 없다. 해당 API는 회장단 검사를 그대로 쓴다.
- 접수 마감과 열람 만료는 별개다. 마감된 회차도 권한이 유효하면 열람할 수 있다.

## 오류 코드

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `FOR158-403` | 403 | 권한 없음·회수됨·만료됨, 또는 같은 회차 지원자의 열람 시도 |
| `FOR159-409` | 409 | 이미 유효한 권한이 있는 사람에게 다시 부여 |
| `FOR160-409` | 409 | 명단 밖·ADMIN 계정 없음·같은 회차 지원자에게 부여 시도 |
| `FOR002-400` | 400 | `expires_at`이 없거나 과거 |
| `FOR148-404` | 404 | 회차 없음 |
| `FOR020-403` | 403 | 회장단이 아닌 사용자의 부여·회수·후보 조회 |

## 검증용 시나리오

1. 회장단으로 후보를 조회하면 해당 학기 운영진이 보인다. ADMIN 계정이 없는 사람은 `has_admin_account: false`다.
2. ADMIN 계정이 있는 후보에게 미래 시각으로 부여하면 `ACTIVE`가 된다.
3. 그 계정으로 로그인해 지원서 목록·상세를 호출하면 200이다.
4. 회장단이 회수하면 같은 계정의 다음 요청은 403(`FOR158`)이다.
5. 만료 시각을 과거로 두고 부여할 수는 없으므로, 만료 동작은 짧은 만료(예: 1분 뒤)로 부여한 뒤 기다려 확인한다.
6. 같은 회차에 지원한 계정에게 부여하면 409(`FOR160`)다.
