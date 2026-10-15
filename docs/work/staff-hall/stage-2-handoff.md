# 2단계 frontend 인계 — 한지석

```text
계약 ID: staff-hall-contract-r2 (endpoint·상태 계약 변경 없음)
backend 기준: 로컬 dev + 미커밋 2단계 구현
연동 상태: backend 구현 완료 / 실제 DB·서버 연동 검증 대기
```

## 구현한 endpoint

| method·path | 권한 | 비고 |
| --- | --- | --- |
| `GET /api/v1/staff-recruitments/current` | public | 게시 회차 없으면 `data: null`, `Cache-Control: no-cache` |
| `GET /api/v1/staff-recruitments/{recruitment_id}` | public | 초안·일정 미설정은 404 (`FOR148-404`) |
| `GET /api/v1/admin/staff-recruitments` | 회장단 | 초안 포함 목록, `CursorPageResponse` |
| `POST /api/v1/admin/staff-recruitments` | 회장단 | 201, 항상 `DRAFT`로 생성 |
| `GET /api/v1/admin/staff-recruitments/{id}` | 회장단 | 관리용 상세 |
| `PATCH /api/v1/admin/staff-recruitments/{id}` | 회장단 | `version` 필수 |
| `POST /api/v1/admin/staff-recruitments/{id}/publish` | 회장단 | `version` 필수 |
| `POST /api/v1/admin/staff-recruitments/{id}/close` | 회장단 | `version` 필수, `reason` 선택 |
| `POST /api/v1/admin/staff-recruitments/{id}/cancel` | 회장단 | `version` 필수, `reason` 선택 |

관리 API는 `ADMIN` 세션 + 회장/부회장(`affiliation`) 검증을 함께 통과해야 한다. 일반 ADMIN은 `FOR020-403`이다.

## 질문에 대한 답

| 질문 | 답 |
| --- | --- |
| create/patch에 departments·questions를 embedded 배열로 보내는지 | 그렇다. 두 필드 모두 요청 본문 안의 배열이다. |
| 기존 department_id·question_id를 patch body에 포함하는 방식 | **포함해 달라.** 2026-10-15 수정으로 식별자 기준 병합이 됐다. 아래 "부서·문항 병합 규칙"을 참고한다. |
| patch·publish·close·cancel의 version 필수 여부 | 모두 **필수**(`@NotNull`)다. 값이 서버와 다르면 `FOR145-409`로 거절하고 아무것도 바꾸지 않는다. 모든 관리 응답에 최신 `version`이 들어 있다. |
| close·cancel reason 필수 여부 | **선택**이다. 최대 200자이며 응답의 `close_reason`으로 돌려준다. |
| 관리자 목록 sort 필드명 | `sort` 파라미터는 **지원하지 않는다**. 항상 `recruitment_id DESC`로 정렬한다. 지금 보내는 `sort=recruitmentId:DESC`는 무시되며 결과는 같다. |

PATCH는 이름과 달리 **전체 본문을 받는다.** `title`은 필수이고, 보내지 않은 `summary`·`description`·기간은 빈 값으로 덮어쓴다. 지금 어드민 폼이 항상 전체 값을 보내므로 동작은 같다. 부분 수정이 필요해지면 알려 달라.

## 응답에서 확인할 점

- 시각은 계약대로 offset 포함 ISO 8601(`2026-12-01T00:00:00+09:00`)이다. 요청도 같은 형식으로 받는다.
  - 다른 기존 API는 `LocalDateTime`이라 offset이 없다. 이 도메인만 계약에 맞춰 `OffsetDateTime`을 쓴다.
- `application_status`는 저장하지 않고 요청 시각으로 계산한다. `application_open`과 `application_unavailable_reason`을 그대로 쓰면 된다.
- `application_unavailable_reason` 값 집합: `NOT_PUBLISHED`, `PERIOD_NOT_SET`, `UPCOMING`, `CLOSED`, `CANCELED`. 접수 중이면 `null`이다.
- **공개 응답은 `active: false`인 부서를 제외**한다. 필드 자체는 프론트 타입(`StaffRecruitmentDepartment.active`)에 맞춰 공개 응답에도 포함하므로 값은 항상 `true`다. 관리 응답에는 비활성 부서까지 모두 내려준다.
- 공개 응답에는 `version`·`ended_at`·`close_reason`이 없다. 관리 응답에만 있다.
- `semester_label`은 `"27-1"` 형태다.

## 상태 규칙

- 저장 상태 전이: `DRAFT → PUBLISHED → CLOSED`, `DRAFT|PUBLISHED → CANCELED`. 그 밖은 `FOR150-400`이다.
- 게시는 기간이 모두 설정된 초안만 가능하다. 기간이 없으면 `FOR149-400`이다.
- 게시 중 회차가 이미 있으면 다른 회차 게시는 `FOR151-409`다. 서버가 기존 회차를 자동으로 닫지 않는다.
- 종료 시각이 지난 뒤의 마감은 `ended_at`에 `ends_at`을 기록하고, 조기 마감·취소는 처리 시각을 기록한다.
- 경계는 `starts_at <= now < ends_at`이다. 시작 시각에 열리고 종료 시각에 닫힌다.

## 오류 코드

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `FOR148-404` | 404 | 회차 없음, 공개 조회에서 초안·일정 미설정 |
| `FOR149-400` | 400 | 기간 역전, 기간 없이 게시 시도 |
| `FOR150-400` | 400 | 허용되지 않은 상태 전이, 종단 상태 수정 |
| `FOR151-409` | 409 | 이미 게시 중인 회차 존재 |
| `FOR152-400` | 400 | 접수 불가 시점의 제출 (3단계에서 사용) |
| `FOR145-409` | 409 | `version` 불일치, DB 제약 충돌 |
| `FOR164-409` | 409 | 제출된 지원서가 있는데 부서·문항을 지우려 함 |
| `FOR002-400` | 400 | 없는 `department_id`·`question_id`, 한 요청에 같은 부서 이름 두 번, 지울 부서와 같은 이름으로 새로 추가 |
| `FOR001-400` | 400 | 읽을 수 없는 요청 본문 (이전에는 500이었다) |
| `FOR020-403` | 403 | 회장단이 아닌 사용자의 관리 API 호출 |
| `FOR003-400` | 400 | 요청 본문 검증 실패 |

## 검증용 데이터 만드는 법

1. 로컬 MySQL(`forif_dev`)에 `local` 프로파일로 서버를 띄우면 Flyway가 `V5__add_staff_recruitment.sql`을 적용한다.
2. 회장 또는 부회장 `affiliation`을 가진 `ADMIN` 계정으로 로그인한다. 일반 ADMIN으로는 관리 API가 403이다.
3. `POST /api/v1/admin/staff-recruitments`로 초안을 만들고 `publish`로 게시하면 `GET /api/v1/staff-recruitments/current`에 나타난다.
4. 상태별 확인은 기간을 조정해 만든다. 시작 전이면 `UPCOMING`, 기간 중이면 `OPEN`, 종료 시각 이후면 `CLOSED`, `cancel` 호출이면 `CANCELED`다.
5. `data: null`은 게시 중인 회차가 없을 때 그대로 재현된다.

## 결정이 필요한 사항

1. ~~부서·문항 전체 교체 방식~~ → **2026-10-15에 식별자 기준 병합으로 바꿨다.** 아래 규칙을 따른다.
2. **공개 상세의 노출 범위**: 마감·취소된 회차도 `GET /{id}`로 보인다. 과거 회차를 안내 화면에서 계속 보여줄지 확인이 필요하다.
3. **목록 정렬·필터**: 지금은 `status` 필터와 ID 역순만 있다. 화면에 다른 정렬이 필요하면 알려주면 추가한다.
4. **`description`의 null 허용**: 서버는 `description`을 비워 둘 수 있어 `null`이 내려올 수 있다. 프론트 타입은 `description: string`이라 `string | null`로 바꾸거나, 서버에서 빈 문자열로 내려주도록 요청해 달라. 현재 어드민 폼은 항상 문자열을 보내므로 폼으로 만든 데이터에서는 발생하지 않는다.

## 부서·문항 병합 규칙 (2026-10-15 변경)

전체 교체를 쓰지 않는다. 전체 교체는 같은 이름을 유지한 수정이 유니크 제약에 걸렸고,
제출된 지원서가 참조하는 부서를 지울 수 없어 모집 수정이 막혔다.

PATCH의 `departments`·`questions`는 **지금 있어야 할 전체 목록**을 보내되, 유지할 항목에는
받은 식별자를 함께 넣는다.

| 보낸 항목                            | 동작                                                      |
| ------------------------------------ | --------------------------------------------------------- |
| `department_id`·`question_id` 포함   | 그 항목을 수정한다. 식별자가 그대로 유지된다              |
| 식별자 없음                          | 새로 추가한다                                             |
| 목록에서 빠짐                        | 지운다. 제출된 지원서가 있으면 `FOR164-409`로 거절한다    |
| 배열 자체를 `null`                   | 건드리지 않는다                                           |

```json
{
  "version": 3,
  "title": "2026-2 FORIF 운영진 모집",
  "departments": [
    { "department_id": 9, "name": "SW팀", "description": "웹 개발", "display_order": 1, "active": true },
    { "department_id": 10, "name": "기획팀", "description": null, "display_order": 2, "active": false },
    { "name": "디자인팀", "description": "새로 추가", "display_order": 3, "active": true }
  ],
  "questions": null
}
```

주의할 점이 세 가지다.

1. **유지할 항목의 식별자를 빠뜨리면 "삭제 + 새로 추가"로 읽는다.** 지원서가 있으면
   `FOR164-409`, 없으면 이름이 겹쳐 `FOR002-400`이 난다. 수정 폼은 조회 응답의
   `department_id`·`question_id`를 그대로 실어 보내야 한다.
2. **제출이 시작된 뒤에는 부서·문항을 지울 수 없다.** 더 뽑지 않는 부서는 `active: false`로
   숨긴다. 숨긴 부서는 공개 응답에서 빠지고 관리 응답에는 남으며, 이미 제출된 지원서의
   지망 표시는 그대로다.
3. **제출 후에도 제목·요약·기간 수정, 부서 내용 수정, 부서·문항 추가는 된다.**

## 그 밖의 변경

- 읽을 수 없는 요청 본문은 이제 `FOR001-400`이다. 전에는 `FOR100-500`이 나갔다.
