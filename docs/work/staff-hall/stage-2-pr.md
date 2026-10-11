# 2단계 PR 초안 — 운영진 모집 회차 관리·공개 조회

```text
대상 브랜치: dev
작업 브랜치: feature/FOR-<이슈번호>  (이슈 번호 확정 후 생성)
기준 커밋: dev / 21364ff
상태: 로컬 구현 완료, 미커밋 / 전체 빌드·스키마 검증 통과
```

## 제목

```text
운영진 모집 회차 관리·공개 조회 API 추가
```

## 본문

### 문제·결과

회장단이 어드민에서 운영진 모집 회차를 만들고 게시하면, 일반 웹의 배너와 `/club/recruit` 안내가 그 내용과 접수 상태를 그대로 보여 준다. 지금까지 `/club/recruit`는 "모집 기간이 아니다"라는 정적 안내만 있었고 모집 데이터를 서버에서 받을 방법이 없었다.

- 회장단: 회차 생성 → 수정 → 게시 → 마감·취소까지 어드민에서 처리한다.
- 지원 예정자: 게시된 회차의 기간·부서·문항과 지금 지원할 수 있는지를 공개 API로 확인한다.
- 접수 가능 여부는 서버가 판단해 내려주므로 화면이 같은 규칙을 다시 구현하지 않는다.

### 기존 코드 재사용

| 재사용 | 내용 |
| --- | --- |
| `StaffAccountService.requirePresidentTeam` | 회장·부회장 검증을 그대로 호출. 권한 모델을 바꾸지 않았다. |
| `SemesterService.getActive` | 회차 생성 시 활동 학기 기본값. 학기 전환이 과거 회차를 바꾸지 않도록 값으로 복사해 보관한다. |
| `ApiResponse`, `CursorPageResponse`, `ForifException`, `ErrorCode` | 응답·오류 형식을 그대로 사용했다. |
| JPA 포트 + `infrastructure/persistence` 구현 | 기존 도메인과 같은 계층 구조를 따랐다. |

스터디의 `SemesterPhase`와 `tb_user_apply`는 목적이 달라 재사용하지 않았다. 기존 스터디 일정 동작은 건드리지 않았다.

### 새로 추가한 부분

| 구분 | 내용 | 이유 |
| --- | --- | --- |
| 테이블 3개 | `tb_staff_recruitment`, `tb_staff_recruitment_department`, `tb_staff_recruitment_question` | 운영진 모집은 독립된 회차·기간·부서·문항이 필요하고 기존 모델로 표현할 수 없다. |
| 도메인 | `StaffRecruitment` 및 상태 enum 3종 | 상태 전이와 접수 기간 판정을 한곳에 두기 위해서다. |
| API 7개 | 공개 2개, 회장단 관리 5개 | 계약 `staff-hall-contract-r2`의 2단계 범위다. |
| ErrorCode 5개 | `FOR148`~`FOR152` | 계약에서 예약한 번호이며 기존 코드와 충돌하지 않는다. |

지원서·reviewer·명예의 전당은 이번 범위가 아니라 구현하지 않았다.

### 계약·권한

- 공개: `GET /api/v1/staff-recruitments/current`, `GET /api/v1/staff-recruitments/{recruitment_id}`
  - `SecurityConfig`의 공개 GET 목록에 이 두 경로만 추가했다. 다른 인증 규칙은 그대로다.
  - 초안과 일정 미설정 회차는 404로 존재를 드러내지 않는다. 관리 전용 필드(`version`, `ended_at`, `close_reason`)와 비활성 부서는 공개 응답에 넣지 않는다.
- 회장단: `GET/POST /api/v1/admin/staff-recruitments`, `GET/PATCH /{id}`, `POST /{id}/publish|close|cancel`
  - `ADMIN` 세션 + 회장·부회장 검증을 함께 통과해야 한다. 일반 ADMIN은 `FOR020-403`이다.
- 상태 전이는 `DRAFT → PUBLISHED → CLOSED`, `DRAFT|PUBLISHED → CANCELED`만 허용한다.
- 게시 중 회차는 동시에 하나다. 애플리케이션 검사와 `published_slot` 유니크 제약으로 이중 방어하며, 서버가 기존 회차를 자동으로 닫지 않고 `FOR151-409`로 거절한다.
- 접수 경계는 `starts_at <= now < ends_at`(Asia/Seoul)이다. 응답 시각은 계약대로 offset 포함 ISO 8601이며, 이 도메인에서만 `OffsetDateTime`으로 변환한다.
- `version`은 수정·게시·마감·취소에 필수다. 값이 다르면 아무것도 바꾸지 않고 `FOR145-409`로 거절한다.

### 관련 PR

- frontend `dev` `0eb488b` — 어드민 모집 설정 화면과 공개 안내 화면이 이미 이 계약으로 구현돼 있다. 이 PR이 먼저 병합·배포돼야 실제 연결이 가능하다.
- 계약 문서: `docs/work/staff-hall/contract.md` (`staff-hall-contract-r2`)
- 인계 문서: `docs/work/staff-hall/stage-2-handoff.md`

### 검증

| 검증 | 결과 |
| --- | --- |
| `./gradlew.bat compileJava` | 통과 |
| `./gradlew.bat test --tests "*StaffRecruitment*"` | 통과 — 단위 테스트 18개 |
| 엔티티 ↔ V5 SQL 컬럼 대조 | 불일치 없음 |
| `./gradlew.bat build` | 통과 — 테스트 343개, 실패 0·오류 0 |
| `FlywayMigrationSchemaTest` | 통과 — Testcontainers MySQL 8.0.46에 V5 적용 후 엔티티 대조 |

추가한 테스트가 검증하는 것

- 시작 직전·시작·종료 직전·종료 시각의 접수 상태
- 게시하지 않은 회차는 기간 안이어도 접수가 닫혀 있음
- 기간 없이 게시 시도, 기간 역전 생성, 종단 상태 재전이 거절
- 자연 종료와 조기 마감의 `ended_at` 기록 차이
- 게시 중 다른 회차가 있을 때 게시 거절, `version` 불일치 시 수정 거절
- 초안의 공개 상세 404, 게시 회차의 접수 상태·사유 코드

스키마 변경의 자동 검증 지점인 `FlywayMigrationSchemaTest`를 실제 MySQL 컨테이너로 통과시켰다.

### 배포 영향

- migration `V5`는 신규 테이블 3개만 추가하며 기존 테이블·행을 변경하지 않는다. 기존 API 응답도 바뀌지 않는다.
- 구버전 프론트는 새 경로를 호출하지 않으므로 영향이 없다. 백엔드를 먼저 배포해도 된다.
- Flyway는 자동 롤백되지 않는다. 되돌려야 하면 신규 테이블을 비우거나 삭제하는 별도 migration이 필요하다. 지원서가 쌓이기 전인 2단계까지는 데이터 손실 위험이 낮다.
- 환경 변수 추가는 없다.

### 남은 사항

- 로컬 서버 기동 확인과 frontend와의 실제 연동 확인이 남아 있다. (출시 차단 사항)
- 부서·문항은 현재 전체 교체 방식이라 교체 시 ID가 새로 발급된다. 3단계 지원서가 이를 참조하므로 제출 이후의 수정 정책을 먼저 정해야 한다. (3단계 차단 사항)
- 어드민 목록은 `status` 필터와 ID 역순 정렬만 지원한다. 다른 정렬이 필요하면 추가 작업이 필요하다.
- 공개 상세는 마감·취소된 회차도 조회된다. 과거 회차 노출 범위는 확인이 필요하다.
- 어드민 목록은 회차마다 부서·문항을 지연 로딩한다. 모집 회차는 한 해에 몇 건 수준이라 지금은 문제가 없지만, 행이 늘면 id 조회 후 일괄 로딩으로 바꿔야 한다. 페이지네이션과 컬렉션 fetch join을 함께 쓰면 메모리 페이징이 되므로 단순 JOIN FETCH로는 해결하지 않는다.
