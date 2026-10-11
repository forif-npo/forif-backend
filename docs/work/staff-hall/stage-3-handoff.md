# 3단계 frontend 인계 — 한지석

```text
계약 ID: staff-hall-contract-r2 (3단계 endpoint·오류 계약 그대로 구현)
backend 기준: feature/staff-recruitment-settings 이후 3단계 작업 브랜치
연동 상태: backend 구현 완료 / 실제 서버 연동 검증 대기
채택 범위: 즉시 제출만. 임시저장·제출 후 수정·취소·재지원·첨부는 구현하지 않음
```

## 구현한 endpoint

| method·path | 권한 | 비고 |
| --- | --- | --- |
| `POST /api/v1/staff-recruitments/{recruitment_id}/applications` | 로그인 회원 + 이번 학기 활동 부원 | 201, 회차당 1회 |
| `GET /api/v1/staff-recruitments/{recruitment_id}/applications/me` | 로그인 회원 | 본인 제출본, 없으면 404 |
| `GET /api/v1/admin/staff-recruitments/{recruitment_id}/applications` | 회장단 | 목록, 이름 검색·지망 부서 필터 |
| `GET /api/v1/admin/staff-recruitments/{recruitment_id}/applications/{application_id}` | 회장단 | 연락처·전체 답변 포함 |

지원서 API는 모두 `Cache-Control: no-store, private`다. 4단계에서 reviewer 권한이 들어오면 관리 API의 권한 검사 지점만 넓힌다.

## 요청 본문 — 제출

```json
{
  "department_choices": [
    { "department_id": 31, "priority": 1 },
    { "department_id": 32, "priority": 2 }
  ],
  "answers": [{ "question_id": 41, "answer": "지원 동기입니다." }]
}
```

- 지원자 식별값은 본문에 넣지 않는다. 서버가 인증 토큰에서만 얻는다.
- `priority`는 1 또는 2다. 1지망은 필수, 2지망은 생략할 수 있다.
- `answers`는 해당 회차의 문항만 보낼 수 있고, 필수 문항은 빠뜨릴 수 없다.

## 응답

제출 성공(201)은 최소 필드만 내려준다.

```json
{
  "application_id": 101,
  "recruitment_id": 12,
  "status": "SUBMITTED",
  "submitted_at": "2026-12-01T10:00:00+09:00"
}
```

본인 조회와 관리 상세는 같은 모양이다.

```json
{
  "application_id": 101,
  "recruitment_id": 12,
  "applicant_user_id": 2020123456,
  "applicant_name": "홍길동",
  "applicant_email": "hong@hanyang.ac.kr",
  "applicant_phone_num": "01000000000",
  "applicant_department": "컴퓨터소프트웨어학부",
  "applied_departments": [{ "department_id": 31, "name": "기획팀", "priority": 1 }],
  "status": "SUBMITTED",
  "submitted_at": "2026-12-01T10:00:00+09:00",
  "answers": [
    {
      "question_id": 41,
      "question_label": "지원 동기",
      "question_type": "LONG_TEXT",
      "answer": "지원 동기입니다."
    }
  ]
}
```

관리 목록 행에는 연락처와 답변이 없다: `application_id`, `applicant_name`, `applicant_department`, `applied_departments`, `status`, `submitted_at`.

- 개인정보는 **제출 당시 snapshot**이다. 회원이 이름·연락처를 바꿔도 지원서 값은 그대로다.
- `applicant_user_id`는 회원 탈퇴 시 `null`이 될 수 있다. snapshot 이름은 남는다.
- `status`는 현재 `SUBMITTED` 하나뿐이다.

## 목록 페이지네이션

- `page`를 주면 오프셋(0부터), 주지 않으면 `cursor` 방식이다. `size` 기본 20, 최대 100이다.
- 응답은 기존 `CursorPageResponse` 그대로이며 `content`, `next_cursor`, `has_next`, `total_elements`, `current_page`, `total_pages`를 쓴다.
- 정렬은 `application_id DESC` 고정이다. 계약의 `sort` 파라미터(`applicationId`·`applicantName`·`submittedAt`)는 **아직 지원하지 않는다**. 필요하면 알려 달라.
- `department_id` 필터는 1·2지망 모두 일치한다. `search`는 제출 당시 이름만 대상으로 하며 전화·이메일 검색은 제공하지 않는다.

## 오류 코드

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `FOR153-403` | 403 | 이번 학기 활동 부원이 아님 (지원 자격 없음) |
| `FOR154-409` | 409 | 같은 회차에 이미 제출함 |
| `FOR155-404` | 404 | 본인 제출본 없음, 다른 회차의 지원서 조회 |
| `FOR156-400` | 400 | 1지망 누락, 같은 부서 중복, 모집하지 않는 부서 |
| `FOR157-400` | 400 | 다른 회차 문항, 필수 문항 미답변, 최대 길이 초과 |
| `FOR152-400` | 400 | 접수 기간이 아님 (시작 전·마감·취소) |
| `FOR148-404` | 404 | 회차 없음 또는 공개 불가 |
| `FOR020-403` | 403 | 회장단이 아닌 사용자의 관리 API 호출 |

지원 자격 판정은 **현재 활동 학기의 수강 관계(`tb_study_user`) 존재 여부**다. 회비 납부 플래그로 판정하지 않는다. 회장단·멘토·ADMIN도 이 조건을 충족해야 지원할 수 있다.

## 검증용 데이터 만드는 법

1. 2단계 API로 회차를 만들고 게시한다. 부서와 문항을 최소 하나씩 넣는다.
2. 지원자 계정은 현재 활동 학기의 `tb_study_user` 행이 있어야 한다. 없으면 403이 난다.
3. 같은 계정으로 두 번 제출하면 409를 재현할 수 있다.
4. 회차 기간을 과거로 바꾸거나 `close`하면 400(`FOR152`)을 재현할 수 있다.

## 결정이 필요한 사항

1. **제출 후 수정·취소·재지원**: 구현하지 않았다. 채택되면 상태값과 endpoint가 늘어난다.
2. **임시저장**: 구현하지 않았다. 채택하면 저장 충돌 처리와 "제출 목록에서 제외" 규칙이 함께 필요하다.
3. **첨부파일**: 구현하지 않았다. 채택하면 공개 `/files/**`와 분리된 비공개 다운로드 경로가 필요하다.
4. **목록 정렬**: 화면에 제출 시각·이름 정렬이 필요한지 알려 달라.
