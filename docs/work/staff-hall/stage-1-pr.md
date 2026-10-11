# 1단계 PR 초안

## 제목

```text
docs: 운영진 지원·명예의 전당 backend 계약 초안 작성
```

## 본문

```markdown
## 변경 목적과 결과

운영진 지원·명예의 전당 개발 전에 기존 회원·운영진·학기·파일 모델과 권한 경계를 조사하고 `staff-hall-contract-r2` backend 계약 초안을 작성했습니다. 코드와 DB는 변경하지 않았습니다.

## 구현 범위와 재사용

- 현재 활동 학기의 `StudyUser` 존재를 정회원 판정 기준으로 정의
- 인증 principal, `StaffAccountService.requirePresidentTeam`, `ForifTeam`, `StaffAccount`, `SemesterService` 재사용 경계 명시
- 모집 회차·부서·문항·지원서·답변·reviewer·audit와 전시 entry·achievement 최소 모델 설계
- 기존 `tb_user_apply`, 스터디 `SemesterPhase`, 대학 학과 `tb_department`는 전용하지 않음

## API·권한 계약

- 계약 리비전: `staff-hall-contract-r2` (frontend 2단계 검토 및 개인정보 영구 보관 정책 반영)
- 공개 모집·본인 지원·관리 지원서·reviewer·공개/관리 전시 endpoint와 정상/오류 예시 작성
- 회장단, 일반 ADMIN, 회차 reviewer, 정회원 USER 권한 분리
- Asia/Seoul 시간 경계, 페이지네이션, cache, 상태 전이와 오류 번호 FOR148~FOR163 예약안 기록

## DB·마이그레이션

변경 없음. 현재 최고 V4를 확인했으며 구현 시점에 다음 버전을 재확인합니다. 신규 테이블·FK·unique·index와 지원서 영구 보관 계획만 문서화했습니다.

## 검증

- Markdown formatter check
- `git diff --check`
- 코드 변경이 없어 Gradle build와 Docker/MySQL Flyway 검증은 미실행

## 배포 순서·복구·남은 사항

문서만 변경되어 배포 대상이 아닙니다. 임시저장/수정/취소/첨부, reviewer 이해충돌, 전시 선정·편집·철회 정책은 운영 결정 대기입니다. 2단계는 모집 계약 frontend 검토 후 독립 진행할 수 있습니다.
```
