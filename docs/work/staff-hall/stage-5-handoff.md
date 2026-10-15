# 5단계 frontend 인계 — 한지석

```text
계약 ID: staff-hall-contract-r2 (명예의 전당 범위 구현 + 아래 결정 사항)
backend 기준: feature/hall-of-fame-content (4단계 브랜치 위에 쌓음)
frontend 기준: dev / 1d716a6 (명예의 전당 구현 없음)
연동 상태: backend 구현 완료 / frontend 화면 미착수
```

frontend에 명예의 전당 코드가 없어 이 문서가 화면 작업의 기준이다. 필드가 모자라면 화면 요구를 알려 주면 맞춘다.

## endpoint

### 공개 (로그인 불필요)

| method·path | 설명 |
| --- | --- |
| `GET /api/v1/hall-of-fame` | 게시된 항목 목록. `act_year`, `act_semester`, `page`, `size` |
| `GET /api/v1/hall-of-fame/{slug}` | 게시된 항목 상세. 초안·게시 중단은 404 |

- 정렬은 `display_order ASC, entry_id DESC` 고정이다.
- `Cache-Control: public, max-age=60`이다. 게시 중단은 최대 60초 뒤 공개 화면에 반영된다.

### 관리 (회장단)

| method·path | 설명 |
| --- | --- |
| `GET /api/v1/admin/hall-of-fame` | 초안·게시 중단 포함 목록. `status`, `page`, `size` |
| `POST /api/v1/admin/hall-of-fame` | 201, 항상 `DRAFT`로 생성 |
| `GET /api/v1/admin/hall-of-fame/{entry_id}` | 관리 상세. **미리보기도 이 경로를 쓴다** |
| `PATCH /api/v1/admin/hall-of-fame/{entry_id}` | `version` 필수 |
| `POST /api/v1/admin/hall-of-fame/{entry_id}/image` | multipart, 파트 이름 `file`. 응답은 `{image_url, version}` |
| `POST /api/v1/admin/hall-of-fame/{entry_id}/publish` | `version` 필수 |
| `POST /api/v1/admin/hall-of-fame/{entry_id}/unpublish` | `version` 필수 |

관리 응답은 모두 `Cache-Control: no-store, private`다.

## 응답 예시

공개 목록·상세 (`CursorPageResponse`의 `content` 안에 들어간다)

```json
{
  "entry_id": 7,
  "slug": "hong-gildong",
  "member_name": "홍길동",
  "act_year": 2026,
  "act_semester": 2,
  "semester_label": "26-2",
  "headline": "26-2 해커톤 대상",
  "introduction": "본문",
  "image_url": "/api/v1/files/hall-of-fame/images/abc.png",
  "badges": ["해커톤 대상", "멘토"],
  "achievements": [
    {
      "achievement_id": 11,
      "title": "해커톤 대상",
      "description": "2026-1 해커톤",
      "link_url": "https://forif.org",
      "display_order": 1
    }
  ],
  "published_at": "2026-10-15T10:00:00+09:00"
}
```

관리 응답은 위 필드에 `member_user_id`, `status`, `display_order`, `version`, `created_at`, `updated_at`가 더 붙는다. **공개 응답에는 이 값들이 없다.**

- `image_url`은 값이 없으면 `null`이다. 사진 없는 카드 표현이 필요하다.
- `member_user_id`는 회원 탈퇴 시 `null`이 될 수 있다. `member_name`은 남는다.
- `status`: `DRAFT | PUBLISHED | UNPUBLISHED`

## 입력 규칙

| 항목 | 규칙 |
| --- | --- |
| `slug` | 영문 소문자·숫자·하이픈, 3~80자, 앞뒤 하이픈 금지. 중복 시 `FOR162-409` |
| `member_name` | 비우면 `member_user_id`로 연결한 회원 이름을 복사한다. 둘 다 없으면 400 |
| `badges` | 최대 10개, 각 30자 |
| `achievements` | 최대 20개. `link_url`은 **https만** 허용 |
| `display_order` | 0 이상. 공개 목록 정렬에 쓴다 |
| 이미지 | 5MB 이하, `image/*`. 교체하면 이전 파일은 커밋 후 정리된다. 업로드도 `version`을 올리므로 응답의 `version`을 받아 두어야 다음 수정·게시가 409가 되지 않는다 |

배지와 성취는 **전체 목록을 보내 교체**한다. 생략하면 빈 목록으로 교체되니 수정 시 기존 값을 함께 보내야 한다.

## 상태 전이

`DRAFT → PUBLISHED → UNPUBLISHED`, `UNPUBLISHED → PUBLISHED`만 허용한다. 그 밖은 `FOR163-400`이다. 재게시하면 `published_at`이 새 시각으로 갱신된다.

## 오류 코드

| 코드 | HTTP | 조건 |
| --- | --- | --- |
| `FOR161-404` | 404 | 항목 없음, 게시되지 않은 항목의 공개 조회 |
| `FOR162-409` | 409 | slug 중복 |
| `FOR163-400` | 400 | 허용되지 않은 게시 전이 |
| `FOR145-409` | 409 | `version` 불일치 |
| `FOR002-400` | 400 | slug 형식, 배지·성취 개수, https 아닌 링크, 이름 없음 |
| `FOR014-400` | 400 | 이미지 용량·형식 위반 |
| `FOR020-403` | 403 | 회장단이 아닌 사용자의 관리 API 호출 |

## 계약과 다르게 구현한 부분

1. **공개 목록 페이지네이션**: 계약은 `cursor | page`였지만 **offset(`page`)만** 지원한다. 정렬 기준이 `display_order`라 커서 키가 단조롭지 않아 커서가 불안정하다. 커서가 필요하면 정렬 기준을 함께 정하자.
2. **배지 저장 방식**: 계약은 JSON 컬럼이었지만 자식 테이블로 저장한다. API는 문자열 배열 그대로라 화면에는 영향이 없다.
3. **미리보기**: 별도 `preview` 경로를 만들지 않고 관리 상세(`GET /admin/hall-of-fame/{entry_id}`)를 쓴다. 회장단 인증이 필요하므로 초안이 공개로 새지 않는다.
4. **공개 응답 축소**: `member_user_id`, `status`, `display_order`, `version`, `created_at`을 공개 응답에서 뺐다. 화면에 필요하면 알려 달라.

## 검증용 시나리오

1. 회장단으로 생성하면 `DRAFT`다. 이때 공개 목록·상세에는 나오지 않는다.
2. 이미지 업로드 후 `image_url`이 생기고, 관리 상세(미리보기)에서 확인된다.
3. `publish` 후 공개 목록과 `/{slug}` 상세에 나타난다.
4. `unpublish` 후 공개 상세는 404다. 캐시 때문에 목록은 최대 60초 늦게 반영될 수 있다.
5. 같은 slug로 생성하면 409다.
6. 지난 시즌(`act_year`·`act_semester`)으로 필터하면 과거 기록이 그대로 조회된다.

## 2026-10-15 로컬 실서버 검증에서 바뀐 것

- 게시·게시 중단·이미지 업로드 응답이 **올라간 `version`**을 돌려준다. 전에는 한 박자 느린 값이
  나가서, 화면이 응답 값으로 다음 요청을 보내면 `FOR145-409`가 났다. 지금은
  생성 0 → 게시 1 → 중단 2 → 재게시 3으로 이어진다.
- 이미지 업로드 응답이 `{image_url}`에서 `{image_url, version}`으로 바뀌었다.
- 이미 게시된 항목의 재게시는 `FOR163-400`이다. 전에는 `FOR145-409`가 먼저 났다.
- 읽을 수 없는 요청 본문은 `FOR001-400`이다. 전에는 500이었다.
- 전시 이미지 업로드는 실제 S3를 호출하므로 로컬 검증에서 제외했다. 7단계 통합 QA에서 본다.
