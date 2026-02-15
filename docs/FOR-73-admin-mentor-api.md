# FOR-73: 운영진 멘토 계정 관리 API

## 변경 요약

기존 `POST /api/v1/staff/signup` (permitAll) 엔드포인트를 삭제하고, 운영진(ADMIN) 전용 멘토 관리 API로 대체.
스터디 승인 시 멘토 계정이 자동 생성되도록 변경.

---

## API 목록

### 1. 멘토 목록 조회

| 항목 | 내용 |
|------|------|
| Method | `GET` |
| URL | `/api/v1/admin/mentors` |
| 권한 | `ADMIN` |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| cursor | Long | X | 커서 (이전 응답의 nextCursor) |
| size | int | X | 페이지 크기 (기본값: 20) |
| search | String | X | 멘토 이름 또는 스터디명 검색 |

**Response**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "userId": 2024222222,
        "name": "홍길동",
        "affiliation": "인공지능 스터디"
      }
    ],
    "nextCursor": 2024111111,
    "hasNext": true,
    "totalElements": 50
  }
}
```

---

### 2. 멘토 계정 생성

| 항목 | 내용 |
|------|------|
| Method | `POST` |
| URL | `/api/v1/admin/mentors` |
| 권한 | `ADMIN` |

**Request Body**
```json
{
  "userId": 2024222222,
  "password": "forif1234",
  "affiliation": "인공지능 스터디"
}
```

> `name`은 User 테이블의 `userName`에서 자동으로 가져옵니다.

---

### 3. 멘토 정보 수정

| 항목 | 내용 |
|------|------|
| Method | `PATCH` |
| URL | `/api/v1/admin/mentors/{userId}` |
| 권한 | `ADMIN` |

**Request Body** (모든 필드 nullable, 포함된 필드만 수정)
```json
{
  "name": "새이름",
  "password": "newPassword123",
  "affiliation": "새스터디명"
}
```

---

### 4. 멘토 계정 삭제

| 항목 | 내용 |
|------|------|
| Method | `DELETE` |
| URL | `/api/v1/admin/mentors/{userId}` |
| 권한 | `ADMIN` |

---

## 스터디 승인 시 멘토 계정 자동 생성

`PATCH /api/v1/admin/studies/{studyId}/approve` 호출 시:
- primaryMentor, secondaryMentor 모두 멘토 계정이 없으면 자동 생성
- 이미 계정이 있는 멘토는 스킵 (기존 멘토가 새 스터디 개설하는 경우)
- 기본 비밀번호: `forif1234`
- name: User 테이블의 `userName` 사용

---

## 삭제된 항목

| 항목 | 설명 |
|------|------|
| `POST /api/v1/staff/signup` | 보안 문제(permitAll)로 삭제 |
| `StaffSignUpCommand.java` | signup 관련 Command 삭제 |
| `StaffSignUpResult.java` | signup 관련 Result 삭제 |
| `StaffSignupRequest.java` | signup 관련 Request DTO 삭제 |
| `StaffSignupResponse.java` | signup 관련 Response DTO 삭제 |
| SecurityConfig permitAll `/api/v1/staff/signup` | 제거 |

---

## 변경 파일 목록

### 신규
- `application/staff/dto/CreateMentorCommand.java`
- `infrastructure/persistence/staff/StaffAccountQueryRepository.java`
- `web/staff/dto/CreateMentorRequest.java`
- `web/staff/dto/MentorResponse.java`
- `web/staff/dto/UpdateMentorRequest.java`

### 수정
- `application/staff/StaffAccountService.java`
- `application/study/StudyService.java` (approveStudy에 멘토 자동 생성 추가)
- `common/config/SecurityConfig.java`
- `domain/staff/StaffAccount.java` (updateInfo 메서드 추가)
- `domain/staff/StaffAccountRepository.java`
- `infrastructure/persistence/staff/StaffAccountRepositoryImpl.java`
- `web/staff/StaffAccountController.java`
- `web/staff/StaffDtoMapper.java`
