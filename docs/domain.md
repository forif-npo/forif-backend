# 도메인 모델과 업무 흐름

FORIF 는 학기 단위로 운영되는 중앙동아리입니다. 도메인 전체가 **"지금이 몇 년도 몇 학기인가"** 를 축으로 돌아갑니다.

## 도메인 목록

| 패키지 | 다루는 것 |
|---|---|
| `semester` | 활동 학기, 학기 일정, 학기 전환 |
| `study` | 스터디 개설·수강·출석·수료증 |
| `user` | 부원 계정, 수강 신청 |
| `staff` | 운영진 계정, 회장단 권한 |
| `dues` | 회비·구글폼 확인, 등록 철회 |
| `hackathon` | 해커톤 참가·팀·제출물·평가·수상 |
| `team` | 역대 운영진 이력 |
| `department` | 단과대학·학과 카탈로그 |
| `post` | 공지사항, FAQ |
| `product` | 부원 서비스 쇼케이스 |
| `auth` | 리프레시 토큰, 토큰 블랙리스트 |

## 학기

`tb_active_semester` 에 **행이 하나뿐**이며 그것이 현재 활동 학기입니다. `SemesterService.getActive()` 로 읽습니다.

학기는 다섯 단계로 나뉘고, 각 단계는 시작·종료 시각을 갖습니다.

```
멘토 모집 ──▶ 멘토 수락/거절 ──▶ 멘티 모집 ──▶ 멘티 수락/거절 ──▶ 스터디 시작
```

| 단계 | enum | 열리는 기능 |
|---|---|---|
| 멘토 모집 | `MENTOR_RECRUIT` | 스터디 개설 신청 |
| 멘토 수락/거절 | `MENTOR_REVIEW` | 운영진의 개설 승인·반려 |
| 멘티 모집 | `MENTEE_RECRUIT` | 수강 신청 |
| 멘티 수락/거절 | `MENTEE_REVIEW` | 멘토의 합격·불합격 처리 |
| 스터디 시작 | `STUDY_START` | 승인된 스터디를 개설 상태로 전환하는 기준일 |

**모집과 심사는 겹칠 수 없습니다.** 모집 창구가 열려 있는 동안 심사가 진행되면 나중에 지원한 사람이 불리해지기 때문입니다. `SemesterScheduleService.validateOrder` 가 순서와 겹침을 검증합니다.

`SemesterPhaseGuard` 가 각 기능의 진입점에서 해당 단계가 열려 있는지 확인합니다. 마감 시각과 경합할 수 있는 합불 처리는 `requireOpenForUpdate` 로 일정 행을 비관적 잠금한 뒤 검사합니다.

일정이 없는 단계는 **닫힌 것으로 봅니다.** 멘티 모집 일정이 없으면 수강 신청도 닫힙니다.

## 스터디

### 개설 흐름

```
멘토가 신청          운영진 심사              학기 시작일
    │                    │                        │
    ▼                    ▼                        ▼
 PENDING ──approve──▶ APPROVED ──scheduler──▶ STARTED
    │                    ▲
    └──reject──▶ REJECTED ──reApply──▶ RE_APPLIED ──approve──┘
```

| 상태 | 의미 |
|---|---|
| `PENDING` | 개설 신청 접수, 심사 대기 |
| `APPROVED` | 승인됨. 아직 개설 전 |
| `STARTED` | 실제 개설됨. `StudyStartScheduler` 가 전환 |
| `REJECTED` | 반려됨. 사유(`rejectReason`) 필수 |
| `RE_APPLIED` | 반려 후 수정해 재신청. `REJECTED` 에서만 가능 |

상태 전이는 `Study` 엔티티의 메서드(`approve`, `reject`, `start`, `reApply`)가 담당하고, 허용되지 않은 전이는 예외를 던집니다. 반려 사유 없이 `reject` 하면 `REJECT_REASON_REQUIRED` 입니다.

`Study` 에는 `@Version` 낙관적 락이 걸려 있습니다. 신청서 수정과 승인 요청이 경합할 때 마지막 저장이 상태를 되돌리지 않게 하기 위함입니다. 충돌하면 `STUDY_APPLICATION_UPDATE_CONFLICT`(409)로 변환됩니다.

### 멘토

스터디는 1순위·2순위 멘토를 가질 수 있습니다. `tb_study.primary_mentor_id` · `secondary_mentor_id` FK 가 기준입니다.

`tb_mentor_study` 는 **레거시 매핑 테이블**입니다. 현재 쓰지 않으며 과거 데이터만 남아 있습니다. 새 코드에서 참조하지 마세요.

멘토 여부 판정은 `Study.isMentor(userId)` 입니다. 레거시 스터디는 멘토가 이름 문자열로만 남고 FK 가 없을 수 있어 `null` 검사가 들어 있습니다.

### 자율부원

정규 스터디를 수강하지 않고 활동하는 부원을 위한 특수 스터디입니다.

- `tb_study.autonomous_flag = true` 이며 학기당 하나만 존재합니다 (`uk_study_autonomous_semester`).
- 개설 신청·심사를 거치지 않고 운영진이 직접 만듭니다. 만든 운영진이 대표 멘토가 됩니다.
- **출석 체크와 수료증 발급 대상이 아닙니다.**
- 합불 처리도 일반 멘토가 아니라 운영진 전용 경로(`rejectAutonomousStudyApplications` 등)를 씁니다.

예약 이름(`자율부원`, `자율스터디`)은 일반 스터디가 쓸 수 없습니다. `Study.isAutonomousStudyName` 이 검사하며, **부분 수정에서 이름을 보내지 않는 요청이 정상이므로 `null` 을 먼저 걸러냅니다.**

### 수강 신청

한 부원은 학기당 신청서 한 장(`tb_user_apply`)을 갖고, 1순위·2순위를 담습니다.

| 컬럼 | 의미 |
|---|---|
| `primary_study` · `primary_status` | 1순위 스터디와 심사 결과 |
| `secondary_study` · `secondary_status` | 2순위 (선택) |
| `pay_status` | 회비 납부 여부 |

상태는 `PENDING` · `ACCEPT` · `REJECT` 입니다.

합불 처리는 멘토가 자기 스터디에 대해서만 할 수 있습니다. **1순위를 합격시키면 2순위는 자동으로 불합격 처리**됩니다. 심사 기간이 끝나면 `PendingApplicationRejectionScheduler` 가 남은 `PENDING` 을 `REJECT` 로 확정합니다.

### 부원 등록과 회비

합격했다고 바로 수강생이 되는 것은 아닙니다.

```
합격(ACCEPT) ──▶ MemberSemesterCheck 생성 ──▶ 회비 납부 + 구글폼 제출 확인 ──▶ StudyUser 등록
```

`tb_member_semester_check` 가 (부원, 학기) 단위로 `dues_paid` · `google_form_submitted` · `registration_withdrawn` 을 들고 있습니다. **세 조건이 모두 만족돼야** `DuesService.synchronizeStudyMembership` 이 `tb_study_user` 에 수강 관계를 만듭니다.

**등록 철회**는 합격 결과를 유지한 채 이번 학기 활동만 취소하는 것입니다. `registration_withdrawn = true` 로 표시하고 수강 관계를 지웁니다. 이후 회비·구글폼 상태가 바뀌어도 다시 등록되지 않습니다.

> 과거에는 명단에서 빼려면 합격 상태를 `REJECT` 로 되돌렸습니다. 그러면 **심사 불합격자와 구분이 안 돼서** 불합격 안내 문자가 잘못 나갈 수 있었습니다. 지금은 플래그로 분리합니다.

### 출석과 수료증

`tb_study_attendance` 가 회차별 출석(`PRESENT` / `ABSENT`)을 기록합니다. 멘토가 일괄 저장합니다.

수료증은 `CertificateService` 가 이미지로 합성해 발급합니다. 템플릿과 폰트는 `src/main/resources/certificate/` 에 있고, 회장 서명 이미지는 `StaffAccount.signature_object_key` 에서 가져옵니다.

자율부원은 수료증 발급 대상이 아닙니다.

## 해커톤

가장 큰 도메인입니다. 컨트롤러 하나가 41개 엔드포인트를 갖습니다.

```
RECRUITING ──▶ TEAM_BUILDING ──▶ IN_PROGRESS ──▶ JUDGING ──▶ ENDED
```

상태 전환은 `HackathonService` 의 스케줄러가 일정에 따라 자동으로 하거나 운영진이 수동으로 합니다.

| 단계 | 열리는 기능 |
|---|---|
| `RECRUITING` | 참가 등록·취소 |
| `TEAM_BUILDING` | 팀 생성, 가입 신청·승인, 팀 해산 |
| `IN_PROGRESS` | 결과물 제출·수정 |
| `JUDGING` | 평가 기준별 점수 제출 |
| `ENDED` | 수상 결과 공개, 아카이브 조회 개방 |

| 테이블 | 내용 |
|---|---|
| `tb_hackathon_event` | 해커톤 회차. (연도, 학기) 와 기수가 각각 유일 |
| `tb_hackathon_participant` | 참가자 (`REGISTERED` / `CANCELED`) |
| `tb_hackathon_team` · `_member` | 팀 (`FORMING` / `CONFIRMED` / `DISBANDED`), 팀원 역할(`LEADER` / `MEMBER`) |
| `tb_hackathon_join_request` | 팀 가입 신청 |
| `tb_hackathon_submission` · `_tech_stack` | 결과물과 기술 스택 |
| `tb_hackathon_evaluation_criterion` · `_evaluation` · `_evaluation_score` | 평가 기준·평가·항목별 점수 |
| `tb_hackathon_award` | 수상 결과 |

경쟁 부문은 `IDEATHON` 과 `HACKATHON` 두 가지이고, 평가자는 `PARTICIPANT`(참가자 상호평가)와 `ADMIN`(운영진 심사)으로 나뉩니다.

아카이브(`/api/v1/archive/**`)는 종료된 해커톤만 공개하며 인증이 필요 없습니다.

## 운영진

### 계정

`tb_staff_account` 가 운영진 로그인 계정입니다. **학기 컬럼이 없습니다** — 계정의 생존 여부일 뿐 "언제 운영진이었나"를 담지 않습니다.

`role` 은 `ADMIN` 과 `MENTOR` 두 값이지만 **`MENTOR` 는 레거시 호환용**이고 신규 계정·토큰에는 쓰지 않습니다.

회장단 판정은 `affiliation` 문자열이 `"회장"` 또는 `"부회장"` 인지를 봅니다. 회장 전용 작업(학기 전환, 회장 위임)은 `"회장"` 만 통과합니다.

### 학기별 명단

`tb_forif_team` 이 **학기별 운영진 명단**입니다. `(act_year, act_semester, user_id)` 와 직책·소속팀·자기소개를 담습니다. "몇 학기에 누가 운영진이었나"의 유일한 근거입니다.

일반 운영진은 본인의 소개 태그·자기소개·졸업년도만 수정할 수 있고, 직책·팀 변경과 타인 프로필 수정은 회장단만 가능합니다.

### 학기 전환

`SemesterTransitionService` 가 활동 학기를 다음 학기로 넘깁니다. 회장 위임이 함께 걸려 있어 **회장만** 실행할 수 있습니다. 변경 이력은 `tb_semester_change_log` 에 남습니다.

## 학과 카탈로그

`tb_college`(16개) · `tb_department`(71개) 가 한양대 단과대학·학과 기준 데이터입니다. 각각 학교 시스템의 `source_code` 를 함께 보관합니다.

`tb_user.department_id` FK 가 정규화된 학과이고, 기존 `department` 문자열 컬럼은 **하위 호환을 위해 함께 유지**합니다. `User.updateDepartment` 가 둘을 동시에 씁니다.

회원가입·프로필 수정은 `department_id` 를 받는 것이 원칙이며, 전환 기간 동안은 학과명 문자열도 허용합니다.

## 알림

카카오 알림톡을 Solapi 로 보냅니다. 수신자 목록은 목적별로 나뉩니다.

| 대상 | 의미 |
|---|---|
| `CURRENT_SEMESTER_MEMBERS` | 현재 학기 수강생 |
| `CURRENT_SEMESTER_APPLICANTS` | 현재 학기 신청자 전원 |
| `CURRENT_SEMESTER_RESOLVED_APPLICANTS` | 심사가 끝난 신청자 |
| `CURRENT_SEMESTER_REGULAR_STUDY_ACCEPTED_APPLICANTS` | 정규 스터디 합격자 |
| `CURRENT_SEMESTER_AUTONOMOUS_STUDY_ACCEPTED_APPLICANTS` | 자율부원 합격자 |
| `CURRENT_SEMESTER_REJECTED_APPLICANTS` | 불합격자 |
| `PREVIOUS_SEMESTER_MEMBERS` | 직전 학기 수강생 |
| `ALL_MEMBERS` | 전체 부원 |
| `ACCEPTED_DUES_UNPAID` | 합격했으나 회비 미납 |
| `ACCEPTED_GOOGLE_FORM_NOT_SUBMITTED` | 합격했으나 구글폼 미제출 |

모든 목록은 **전화번호가 있는 사용자만** 포함합니다. 등록 철회자는 회비·구글폼 독촉 대상에서 제외됩니다.

발송은 수신자별로 실패를 격리합니다. 한 명의 조회가 실패해도 나머지에게는 발송되고, 실패한 수신자는 결과 목록에 사유와 함께 표시됩니다.
