# 아키텍처

## 계층 구조

의존은 한 방향으로만 흐릅니다.

```
web  ──▶  application  ──▶  domain  ◀──  infrastructure
                              ▲
                              └── 모든 계층이 domain 을 향한다
```

| 계층 | 위치 | 역할 | 파일 수 |
|---|---|---|---|
| `web` | `org.forif_backend.web` | 컨트롤러, 요청·응답 DTO, 매퍼. HTTP 관심사만 다룹니다 | 133 |
| `application` | `org.forif_backend.application` | 서비스, 유스케이스, 아웃바운드 포트 인터페이스 | 98 |
| `domain` | `org.forif_backend.domain` | JPA 엔티티, 도메인 규칙, 리포지토리 인터페이스 | 76 |
| `infrastructure` | `org.forif_backend.infrastructure` | JPA·Redis 구현체, 외부 연동 | 59 |
| `common` | `org.forif_backend.common` | 인증 필터, 공통 응답·예외, 설정, 유틸 | 26 |

### 지켜야 할 것

- **`domain` 은 아무것도 참조하지 않습니다.** Spring, JPA 애노테이션 외의 바깥 계층을 알면 안 됩니다.
- **`application` 은 `web` 을 모릅니다.** 서비스가 web DTO 를 반환하면 위배입니다. `application/*/dto` 의 자체 DTO 를 쓰세요.
- **`infrastructure` 는 `domain` 의 인터페이스를 구현합니다.** 예: `domain.study.StudyRepository` ← `infrastructure.persistence.study.StudyRepositoryImpl`
- **외부 연동은 포트를 거칩니다.** `application/*/port/out` 에 인터페이스를 두고 `infrastructure/external` 에서 구현합니다.

현재 아웃바운드 포트는 두 개입니다.

| 포트 | 구현체 | 용도 |
|---|---|---|
| `FilePort` | `LocalFileClient` | 파일 저장·조회 |
| `NotificationSendPort` | `NotificationClient` | 카카오 알림톡 발송·이력 조회 |

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| 언어 · 런타임 | Java 17 |
| 프레임워크 | Spring Boot 3.5.5 (Web MVC, Security, Data JPA, Validation) |
| 데이터베이스 | MySQL 8.0 (JPA + QueryDSL 5.1.0) |
| 캐시 | Redis 7 — 리프레시 토큰 저장, 로그아웃 토큰 블랙리스트 |
| 스키마 관리 | Flyway (`flyway-core`, `flyway-mysql`) |
| 인증 | JWT (jjwt 0.11.5) + Google OAuth |
| 알림 | Solapi SDK 1.0.3 (카카오 알림톡) |
| API 문서 | springdoc-openapi 2.8.6 (Swagger UI, Scalar) |
| 빌드 | Gradle 8.14.3 |
| 테스트 | JUnit 5, H2, Testcontainers |

`spring-boot-starter-webflux` 와 Netty 가 의존성에 있지만 실제 사용처는 `GoogleOAuthClientImpl` 한 곳입니다. 애플리케이션 본체는 서블릿(Web MVC) 기반입니다.

## 공통 규약

### 응답 래퍼

모든 응답은 `ApiResponse<T>` 로 감쌉니다.

```json
{
  "timestamp": 1757000000000,
  "data": { },
  "error_code": null,
  "message": "Success"
}
```

예외는 파일 서빙(`GET /api/v1/files/**`)뿐입니다. 파일 바이트를 직접 반환해야 하므로 `ResponseEntity<Resource>` 를 씁니다.

### 예외 처리

도메인 예외는 `ForifException(ErrorCode)` 하나로 통일합니다. 컨트롤러에서 `try-catch` 로 삼키지 말고 던지세요. `GlobalExceptionHandler` 가 받아 `ApiResponse` 형태로 변환합니다.

`ErrorCode` 는 `(HttpStatus, code, message)` 를 갖는 enum 이고 코드 형식은 `FOR{번호}-{HTTP상태}` 입니다. 현재 117개이며 `GET /api/v1/error-codes` 가 전체 목록을 반환합니다.

| HTTP | 개수 |
|---|---|
| 400 | 63 |
| 404 | 28 |
| 409 | 12 |
| 403 | 7 |
| 500 | 4 |
| 401 | 3 |

전역 핸들러가 별도로 처리하는 예외입니다.

| 예외 | 변환 결과 |
|---|---|
| `ObjectOptimisticLockingFailureException` | `STUDY_APPLICATION_UPDATE_CONFLICT` (409) |
| `DataIntegrityViolationException` | `DATA_INTEGRITY_VIOLATION` (409, DB 제약명·SQL 미노출) |
| `MethodArgumentNotValidException` | `VALIDATION_FAILED` (필드별 오류 목록 포함) |
| `MissingServletRequestParameterException` | `MISSING_PARAMETER` |
| `MethodArgumentTypeMismatchException` | `TYPE_MISMATCH` |
| `AccessDeniedException` | `INSUFFICIENT_PERMISSION` (403) |
| `MaxUploadSizeExceededException` | `INVALID_FILE_ATTACHMENT` |
| 그 외 `Exception` | `INTERNAL_SERVER_ERROR` (500) |

### 직렬화

- **요청·응답 바디는 `SNAKE_CASE`** 입니다 (`spring.jackson.property-naming-strategy`).
- **쿼리 파라미터는 이 설정의 영향을 받지 않습니다.** 필요하면 `@RequestParam("dues_paid")` 처럼 직접 지정하세요. 현재 코드에 `snake_case` 와 `camelCase` 가 섞여 있습니다.
- 시간대는 `Asia/Seoul` 고정입니다. 코드에서는 `DateUtils.ZONE_SEOUL` 을 씁니다.

### 페이지네이션

`CursorPageResponse<T>` 가 커서 방식과 페이지 방식을 겸합니다.

| 필드 | 의미 |
|---|---|
| `content` | 목록 |
| `next_cursor` | 다음 요청의 `cursor` 로 넘길 값 |
| `has_next` | 다음 페이지 존재 여부 |
| `total_elements` · `current_page` · `total_pages` | offset 모드용 |

쿼리 파라미터는 `cursor`(nullable), `page`(nullable, 0부터), `size`(기본 20)입니다. **둘 다 생략하면 커서 모드 첫 페이지**입니다.

정렬은 `SortCriteria.parse(sort, 허용컬럼)` 로 `"field:ASC|DESC"` 형식을 파싱하며, 허용 컬럼 화이트리스트 밖이면 `INVALID_INPUT` 을 던집니다.

### 트랜잭션과 지연로딩

**`open-in-view: false`** 입니다. 컨트롤러에서 지연로딩을 건드리면 `LazyInitializationException` 이 납니다. 필요한 데이터는 서비스 계층 트랜잭션 안에서 DTO 로 옮기세요.

`UserService`, `HackathonService` 등 조회가 많은 서비스는 클래스 레벨 `@Transactional(readOnly = true)` 를 두고, 변경 메서드에만 `@Transactional` 을 다시 답니다.

## 인증과 인가

### 토큰

| 토큰 | 만료 | 전달 방식 |
|---|---|---|
| Access | 1시간 | `Authorization: Bearer` 헤더 |
| Refresh | 30일 | `refreshToken` 쿠키 (HttpOnly, Secure, SameSite=Lax) |

서명은 HMAC-SHA (`Keys.hmacShaKeyFor`, BASE64 디코딩된 `${JWT_SECRET}`) 이고, 클레임은 `subject=userId`, `role`, `tokenType`(ACCESS/REFRESH) 입니다.

`POST /api/v1/users/refresh` 가 쿠키를 읽어 토큰 로테이션(새 Access + 새 Refresh)을 수행합니다. 로그아웃은 Access 를 Redis 블랙리스트에 넣고 Refresh 를 삭제한 뒤 쿠키를 만료시킵니다.

### 필터 체인

`JwtAuthenticationFilter` 가 `UsernamePasswordAuthenticationFilter` 앞에 등록됩니다. 검사 순서는 이렇습니다.

1. 토큰 추출 → 2. 만료 확인 → 3. 서명 검증 → 4. ACCESS 타입 확인 → 5. Redis 블랙리스트 확인 → 6. ADMIN 토큰이면 `StaffAccount` 생존 확인

실패해도 예외를 던지지 않고 request 속성 `jwt.error` 에 `ErrorCode` 를 담아 넘깁니다. `JwtAuthenticationEntryPoint` 가 이를 읽어 401 을, `JwtAccessDeniedHandler` 가 403 을 **동일한 `ApiResponse` 포맷으로** 반환합니다.

인증 정보의 principal 은 `Long userId` 이고, 컨트롤러는 `@AuthenticationPrincipal Long userId` 로 받습니다.

### 권한 모델

토큰 `role` 이 `ADMIN` 이면 `ROLE_ADMIN` · `ROLE_MENTOR` · `ROLE_USER` 세 권한을, 아니면 `ROLE_USER` 만 부여합니다.

**컨트롤러의 `@PreAuthorize` 는 65곳 전부 `hasRole('ADMIN')` 입니다.** `hasRole('MENTOR')` 는 한 곳도 없습니다.

멘토 권한은 계정 종류가 아니라 **"이 스터디의 멘토인가"라는 관계**이기 때문입니다. `StudyMentorAccess` 가 서비스 계층에서 `study.isMentor(userId)` 를 검사합니다. 조회는 지난 학기도 허용하고, 변경은 활동 학기로 제한합니다.

회장단 전용 작업은 `StaffAccountService.requirePresidentTeam(userId)` 를 서비스 계층에서 호출해 검사합니다. 회장단 판정은 `StaffAccount.affiliation` 이 `"회장"` 또는 `"부회장"` 인지를 봅니다.

### 무인증 공개 경로

`SecurityConfig` 에서 `anyRequest().authenticated()` 를 기본으로 두고 아래만 엽니다.

| 메서드 | 경로 |
|---|---|
| 무관 | `/swagger-ui/**`, `/v3/api-docs/**`, `/scalar/**`, `/webjars/**`, `/favicon.ico` |
| 무관 | `/api/v1/users/signup`, `/signin`, `/refresh`, `/api/v1/staff/signin` |
| `HEAD` | `/api/v1/files/**` |
| `GET` | `/api/v1/studies`, `/api/v1/products`, `/api/v1/semesters`, `/api/v1/semester-schedules`, `/api/v1/hackathons`, `/api/v1/archive/**`, `/api/v1/files/**`, `/api/v1/forif-team`, `/api/v1/posts/faqs/**`, `/api/v1/posts/announcements/**`, `/api/v1/departments` |

**새 공개 조회 경로는 `GET` 블록에 넣으세요.** 메서드 무관 블록에 넣으면 나중에 같은 경로로 POST 가 추가될 때 인증 없이 열립니다.

### CORS

모든 origin 패턴(`*`)을 허용하고 `allowCredentials(true)` 입니다. 소스에 "프로덕션 환경에서는 특정 도메인만 허용하도록 변경 필요"라는 TODO 가 남아 있습니다.

## 백그라운드 작업

`@EnableScheduling` 이 켜져 있고 스케줄러 네 개가 돕니다. 주기는 모두 프로퍼티로 조정 가능합니다.

| 스케줄러 | 기본 주기 | 하는 일 |
|---|---|---|
| `StudyRecruitStatusScheduler` | 30초 | 멘티 모집 일정에 따라 승인된 스터디의 모집 상태 동기화 |
| `StudyStartScheduler` | 30초 | 스터디 시작 시각이 지난 승인 스터디를 개설 상태로 전환 |
| `PendingApplicationRejectionScheduler` | 60초 | 멘티 합불 기간 종료 뒤 남은 `PENDING` 을 `REJECT` 로 확정 |
| `HackathonService.synchronizeHackathonStatuses` | 30초 | 해커톤 상태 자동 전환 |

**단일 인스턴스 전제입니다.** 스케일아웃하면 중복 실행을 막는 장치(분산 락 등)가 필요합니다.

## 알아둘 함정

이 코드베이스에서 실제로 사고가 났던 지점들입니다.

**Mockito 는 인터페이스의 `default` 메서드를 가로채 `null` 을 반환합니다.** 포트 인터페이스에 `default` 메서드를 추가하면 기존 테스트가 조용히 깨집니다. 공통 로직은 `FileViewUrls` 처럼 정적 유틸리티로 빼세요.

**`Set.of(...)` 로 만든 불변 집합은 `contains(null)` 에서 NPE 를 던집니다.** 부분 수정 요청에서 `null` 이 들어오는 경로를 확인하고 먼저 걸러내세요.

**MySQL 은 DDL 이 트랜잭션이 아닙니다.** 마이그레이션이 중간에 실패하면 앞쪽 DDL 은 커밋된 채 남습니다. 자세한 내용은 [database.md](database.md) 를 보세요.

**`@DataJpaTest` 는 프로파일을 지정하지 않으면 `application.yml` 의 기본값을 씁니다.** 테스트 전용 설정을 `application-test.yml` 에 넣어도 적용되지 않습니다.
