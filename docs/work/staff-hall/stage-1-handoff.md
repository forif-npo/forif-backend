# 1단계 frontend 인계 — 한지석

```text
계약 ID: staff-hall-contract-r2
기준 backend commit/PR: local dev 6e1f75b / PR 미게시
합의 상태: frontend 2단계 검토 완료, 개인정보 영구 보관 반영 / 실제 구현 대기

backend 제안 endpoint (frontend 합의 후 확정):
- GET /api/v1/staff-recruitments/current (public, 없으면 200 data:null)
- GET /api/v1/staff-recruitments/{recruitment_id} (public)
- POST /api/v1/staff-recruitments/{recruitment_id}/applications (USER+정회원)
- GET /api/v1/staff-recruitments/{recruitment_id}/applications/me (본인)
- /api/v1/admin/staff-recruitments 아래 모집·지원서·reviewer 관리
- GET /api/v1/hall-of-fame 및 /{slug} (published만 public)
- /api/v1/admin/hall-of-fame 아래 전시 관리

지원 정책:
- 정회원 = 현재 활동 학기의 tb_study_user 관계가 1개 이상인 인증 회원
- 회차+사용자당 1회, DB unique와 409로 동시 중복도 차단
- 1지망 필수, 2지망 선택, 같은 부서 중복 불가
- application_open은 server 계산; starts_at <= now < ends_at
- 날짜는 offset 포함 ISO 8601, 판정은 Asia/Seoul
- 1차 지원 상태는 SUBMITTED만 제안(임시저장/수정/취소/첨부는 정책 대기)

주요 오류:
- FOR148-404 회차 없음
- FOR151-409 다른 게시 회차 충돌
- FOR152-400 접수 불가
- FOR153-403 정회원 자격 없음
- FOR154-409 중복 지원
- FOR155-404 지원서 없음
- FOR156-400 지망 오류
- FOR157-400 답변 오류
- FOR158-403 reviewer 권한 없음
- FOR159-409 reviewer 중복
- FOR160-409 reviewer/지원자 이해충돌
- FOR161-404 공개 전시 없음

pagination/cache:
- CursorPageResponse, page 없으면 cursor, 기본 size=20/최대100
- 개인정보·admin 응답 Cache-Control: no-store, private
- 공개 모집 no-cache, 공개 전시 public,max-age=60

DB/migration:
- 기존 V1~V4 수정 없음, 구현 직전 다음 버전 재확인
- 신규 recruitment/department/question/application/choice/answer/reviewer/audit
- 신규 hall entry/achievement, badges는 entry JSON
- 실제 migration은 2~5단계에서 기능 단위 추가

개인정보:
- 자연 종료는 ends_at, 조기 마감·취소는 action 시각을 ended_at으로 기록
- application snapshot/답변/지망은 모집 종료와 관계없이 DB에 영구 보관
- 기간 기반 자동 삭제 scheduler/batch는 구현하지 않음
- 감사에는 답변·연락처·이름을 저장하지 않음

frontend 변경 요청:
- r0의 2지망 필수 여부를 optional로 반영
- current 없음은 200 + data:null 처리
- 422 후보는 사용하지 않고 기존 backend 관례의 400/403/409로 처리
- status와 endpoint 이름을 contract.md 기준으로 검토
- applicant_user_id는 관리자/본인 상세에만 사용하고 공개 영역에 노출하지 않기

검증 데이터 선행 조건:
- active semester 설정
- 해당 학기 StudyUser 관계가 있는 USER와 없는 USER
- ADMIN StaffAccount + 같은 학기 ForifTeam 후보
- 회장/부회장 affiliation ADMIN
- DRAFT/PUBLISHED/CLOSED/CANCELED 회차 fixture

미결정 운영 정책:
- 임시저장/수정/취소/재지원/첨부 제외 승인
- 지원자와 reviewer 이해충돌 규칙 승인
- 전시 선정·공개 동의·철회·일반 ADMIN 편집 허용 여부
- 공개 전시 cache 60초 허용 여부
```

전체 request/response, 상태 전이, 권한 매트릭스와 migration 설계는 `contract.md`를 정본으로 본다.
