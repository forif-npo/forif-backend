# 5단계 PR 초안 — 명예의 전당 콘텐츠 관리·게시

```text
대상 브랜치: feature/staff-recruitment-reviewer (상위 PR 머지 시 자동으로 올라감)
작업 브랜치: feature/hall-of-fame-content
선행 PR: #161(2단계) → #162(3단계) → #163(4단계)
상태: 구현 완료 / 전체 빌드·스키마 검증 통과 / frontend 화면 미착수
```

## 제목

```text
명예의 전당 전시 콘텐츠 관리·공개 조회 API 추가
```

## 본문

### 문제·결과

회장단이 명예의 전당에 올릴 인물과 성취를 직접 등록하고, 게시한 항목만 일반 웹에 공개한다. 지금까지 명예의 전당은 화면도 데이터도 없었다.

- 회장단: 항목을 초안으로 만들고 이미지·배지·성취를 채운 뒤 미리보기로 확인하고 게시한다. 게시 중단도 할 수 있다.
- 방문자: 게시된 항목만 시즌별로 보고, 주소(slug)로 상세를 연다.
- 자동 점수·랭킹은 이번 범위가 아니다.

### 기존 코드 재사용

| 재사용 | 내용 |
| --- | --- |
| `FilePort`, `FileViewUrls`, `TransactionalFileCleanup` | 전시 이미지 저장·조회 URL·트랜잭션 정리를 기존 규칙 그대로 사용 |
| `StaffAccountService.requirePresidentTeam` | 관리 API 권한 검사 |
| `ApiResponse`, `CursorPageResponse`, `ErrorCode` | 응답·목록·오류 형식 |
| `tb_user` | 전시 인물을 기존 회원 ID로 연결 (연결 없이 이름만으로도 등록 가능) |

### 새로 추가한 부분

| 구분 | 내용 | 이유 |
| --- | --- | --- |
| 테이블 3개 | `tb_hall_of_fame_entry`, `..._badge`, `..._achievement` | 전시 항목과 배지·성취의 제약이 서로 다르다. |
| 도메인 | `HallOfFameEntry`, `HallOfFameAchievement`, `HallOfFameStatus` | 주소 형식·게시 전이·개수 제한을 한곳에서 지킨다. |
| API 9개 | 공개 2개, 회장단 7개 | 계약의 5단계 범위다. |
| ErrorCode 3개 | `FOR161`~`FOR163` | 계약에서 예약한 번호다. |

### 계약·권한

- 공개: `GET /api/v1/hall-of-fame`, `GET /api/v1/hall-of-fame/{slug}`
  - `SecurityConfig`의 공개 GET 목록에 이 두 경로만 추가했다.
  - **게시된 항목만** 반환한다. 초안·게시 중단은 slug를 알아도 404다.
  - 공개 응답에는 `member_user_id`, `status`, `display_order`, `version`, `created_at`을 넣지 않는다.
  - `Cache-Control: public, max-age=60`. 게시 중단이 최대 60초 뒤 반영된다.
- 회장단: 목록·생성·상세·수정·이미지·게시·게시 중단. 모두 `no-store, private`다.
  - 미리보기는 별도 경로 없이 관리 상세를 쓴다. 인증이 필요하므로 초안이 공개로 새지 않는다.
- 상태 전이는 `DRAFT → PUBLISHED → UNPUBLISHED`, `UNPUBLISHED → PUBLISHED`만 허용한다. 재게시하면 `published_at`이 갱신된다.
- 입력 제한: slug는 영문 소문자·숫자·하이픈 3~80자(앞뒤 하이픈 금지), 배지 10개·각 30자, 성취 20개, 외부 링크는 https만, 이미지 5MB 이하 `image/*`.
- 회원 연결은 선택이다. 연결하면 이름을 복사해 두고, 탈퇴 시 FK만 `ON DELETE SET NULL`로 끊어 게시된 소개는 유지한다.

### 관련 PR

- 선행: #161 → #162 → #163. 이 PR은 #163 브랜치 위에 쌓았다.
- frontend에는 아직 명예의 전당 화면이 없다. 계약과 응답 예시는 `docs/work/staff-hall/stage-5-handoff.md`에 정리했다.

### 검증

| 검증 | 결과 |
| --- | --- |
| `./gradlew.bat build` | 통과 — 테스트 387개, 실패 0·오류 0 |
| `FlywayMigrationSchemaTest` | 통과 — V5~V8을 실제 MySQL 8.0.46에 적용 후 엔티티 대조 |
| `HallOfFameEntryTest` | 통과 — 8개 |
| `HallOfFameServiceTest` | 통과 — 8개 |
| 엔티티 ↔ V8 SQL 컬럼 대조 | 불일치 없음 (배지 컬렉션 테이블 포함) |

테스트가 검증하는 것: 잘못된 slug 거절, 초안의 비공개, 게시 시각 기록, 중복 게시·게시 중단 전이 거절, 재게시 시각 갱신, 배지 10개·30자 제한, 성취 20개 제한, 이름 자동 복사, slug 중복 409, https 아닌 링크 거절, `version` 불일치 409, 비공개 항목의 공개 조회 404.

### 배포 영향

- migration `V8`은 신규 테이블 3개만 추가한다. 기존 테이블·행·API 응답은 변하지 않는다.
- 공개 GET 2개가 늘지만 게시된 데이터가 없으면 빈 목록과 404만 응답한다.
- 이미지 저장 경로는 `hall-of-fame/images`로 분리했다. 기존 파일 제공 규칙은 바꾸지 않았다.
- Flyway는 자동 롤백되지 않는다. 되돌리려면 테이블을 제거하는 별도 migration이 필요하다.

### 남은 사항

- frontend 화면이 아직 없다. 실제 콘텐츠로 공개 화면을 만들고 연동 확인하는 것이 6단계다. (출시 차단)
- 공개 목록은 offset 페이지네이션만 지원한다. 계약의 커서 방식은 정렬 기준이 `display_order`라 키가 단조롭지 않아 제외했다.
- 배지는 계약의 JSON 컬럼 대신 자식 테이블로 저장한다. API 모양은 문자열 배열 그대로다.
- 게시 중단의 공개 반영이 최대 60초 늦다. 즉시 철회가 필요하면 캐시 정책을 다시 정해야 한다.
