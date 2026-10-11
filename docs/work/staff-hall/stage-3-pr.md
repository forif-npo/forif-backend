# 3단계 PR 초안 — 운영진 지원서 제출·조회

```text
대상 브랜치: feature/staff-recruitment-settings (2단계 PR #161 머지 후 dev로 자동 변경됨)
작업 브랜치: feature/staff-recruitment-application
선행 PR: #161 운영진 모집 회차 관리·공개 조회 API 추가
상태: 구현 완료 / 전체 빌드·스키마 검증 통과 / 실제 서버 연동 대기
```

## 제목

```text
운영진 지원서 제출·조회 API 추가
```

## 본문

### 문제·결과

게시된 모집 회차에 부원이 지원서를 내고, 회장단이 제출된 지원서를 확인할 수 있게 한다. 2단계까지는 모집 공고만 보여 줄 수 있었고 지원을 받을 방법이 없었다.

- 이번 학기 활동 부원: 지망 부서와 자기소개서를 제출하고, 본인 제출본을 다시 확인한다.
- 회장단: 회차별 지원자 목록을 보고 상세에서 연락처와 답변을 확인한다.
- 자격·기간·문항 검증을 서버가 모두 수행해 화면이 같은 규칙을 다시 구현하지 않는다.

### 기존 코드 재사용

| 재사용 | 내용 |
| --- | --- |
| 인증 컨텍스트 `@AuthenticationPrincipal Long userId` | 지원자 식별을 요청 본문에서 받지 않는다. |
| `StudyUserRepository.existsByUserIdAndStudyYearSemester` + `SemesterService.getActive` | 현재 활동 학기의 수강 관계로 지원 자격을 판정한다. |
| `StaffAccountService.requirePresidentTeam` | 관리 API 권한 검사. |
| `CursorPageResponse.paginate` | 기존 목록 API와 같은 커서·오프셋 혼용 방식. |
| 2단계 `StaffRecruitment` | 접수 가능 여부 판정을 그대로 호출한다. 같은 규칙을 다시 쓰지 않았다. |

`tb_user_apply`(스터디 지원)는 목적이 달라 재사용하지 않았고 변경하지도 않았다.

### 새로 추가한 부분

| 구분 | 내용 | 이유 |
| --- | --- | --- |
| 테이블 3개 | `tb_staff_application`, `..._choice`, `..._answer` | 지원서·지망·답변의 제약과 이력이 모두 다르다. |
| 도메인 | `StaffApplication`, `...Choice`, `...Answer`, 상태 enum | 제출 시점 snapshot과 지망·답변 구성을 한곳에서 관리한다. |
| API 4개 | 제출, 본인 조회, 회장단 목록·상세 | 계약 3단계 범위다. |
| ErrorCode 5개 | `FOR153`~`FOR157` | 계약에서 예약한 번호다. |

임시저장·제출 후 수정·취소·재지원·첨부는 **채택되지 않아 구현하지 않았다.** 상태값도 `SUBMITTED` 하나만 두었다.

### 계약·권한

- 지원자: `POST /api/v1/staff-recruitments/{id}/applications`, `GET .../applications/me`
  - 로그인 + **현재 활동 학기의 `tb_study_user` 존재**가 조건이다. 아니면 `FOR153-403`이다.
  - 본인 조회는 회차와 인증 사용자로만 찾는다. ID를 알아도 타인 지원서를 볼 수 없다.
- 회장단: `GET /api/v1/admin/staff-recruitments/{id}/applications`, `GET .../{application_id}`
  - 목록은 연락처·답변을 내려주지 않는다. 상세에만 포함한다.
  - 다른 회차의 지원서 ID로 조회하면 `FOR155-404`다.
  - 4단계 reviewer 권한은 이 컨트롤러의 검사 지점만 넓히면 된다.
- 모든 지원서 응답은 `Cache-Control: no-store, private`다.
- 제출 검증: 1지망 필수, 같은 부서 중복 금지, 모집 중단(`active=false`) 부서 차단, 문항 소속·필수·최대 길이, 접수 기간.
- 중복 제출은 사전 조회와 `(recruitment_id, applicant_user_id)` 유니크 제약으로 이중 방어한다. 동시 요청도 409로 수렴한다.

### 개인정보 처리

- 제출 시점의 이름·이메일·전화·학과를 지원서에 복사해 보관한다. 회원이 프로필을 바꿔도 제출본은 바뀌지 않는다.
- 회원 FK는 `ON DELETE SET NULL`이다. 탈퇴하면 연결만 끊기고 snapshot과 답변은 남는다. 계약 r2의 영구 보관 정책이다.
- 자동 삭제 scheduler·batch는 만들지 않았다.
- 로그·audit에 답변과 연락처를 남기지 않는다.

### 관련 PR

- 선행: #161 (2단계). 이 PR은 #161 브랜치 위에 쌓았고, #161이 `dev`에 머지되면 base가 `dev`로 자동 변경된다.
- frontend: 3단계 화면은 아직 구현 전이다. 계약과 응답 예시는 `docs/work/staff-hall/stage-3-handoff.md`에 정리했다.

### 검증

| 검증 | 결과 |
| --- | --- |
| `./gradlew.bat build` | 통과 — 테스트 355개, 실패 0·오류 0 |
| `FlywayMigrationSchemaTest` | 통과 — 실제 MySQL 8.0.46에 V5·V6 적용 후 엔티티 대조 |
| `StaffApplicationServiceTest` | 통과 — 12개 |
| 엔티티 ↔ V6 SQL 컬럼 대조 | 불일치 없음 |

테스트가 검증하는 것: 자격 미달 403, 중복 제출 409, 접수 기간 외 400, 1지망 누락·부서 중복·모집 중단 부서 400, 필수 문항 미답변·길이 초과·타 회차 문항 400, 제출 시 snapshot·지망·답변 저장, 본인 제출본 없음 404, 다른 회차 지원서 404.

### 배포 영향

- migration `V6`은 신규 테이블 3개만 추가한다. 기존 테이블·행·API 응답은 변하지 않는다.
- 새 경로만 추가하므로 구버전 프론트에 영향이 없다.
- `SecurityConfig` 변경 없음. 지원서 API는 모두 인증이 필요하다.
- Flyway는 자동 롤백되지 않는다. 되돌리려면 신규 테이블을 제거하는 별도 migration이 필요하며, 제출 데이터가 쌓인 뒤에는 데이터 손실을 검토해야 한다.

### 남은 사항

- 실제 서버 기동 후 제출·조회 흐름 확인과 frontend 연동이 남아 있다. (출시 차단)
- 목록 정렬 파라미터(`applicationId`·`applicantName`·`submittedAt`)는 계약에 있으나 구현하지 않았다. 현재는 `application_id DESC` 고정이다.
- 임시저장·수정·취소·재지원·첨부는 정책 확정 후 별도 작업이다.
- 부서·문항을 전체 교체하면 ID가 새로 발급된다. 제출이 쌓인 회차에서는 교체를 막거나 부분 수정 계약으로 바꿔야 한다. (2단계에서 이어진 결정 사항)
