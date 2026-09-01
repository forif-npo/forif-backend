# FORIF Backend

한양대학교 IT 학회 **FORIF** 웹사이트([forif.org](https://forif.org))의 백엔드 서비스입니다.
스터디 개설·수강 신청, 해커톤 운영, 수료증 발급, 운영진 관리 기능을 REST API로 제공합니다.

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| 언어 · 런타임 | Java 17 |
| 프레임워크 | Spring Boot 3.5.5 (Web MVC, Security, Data JPA, Validation) |
| 데이터베이스 | MySQL 8.0 (JPA + QueryDSL 5.1.0) |
| 캐시 · 세션 | Redis 7 (리프레시 토큰 저장, 로그아웃 토큰 블랙리스트) |
| 스키마 관리 | Flyway |
| 인증 | JWT (jjwt 0.11.5) + Google OAuth |
| 알림 | Solapi SDK (카카오 알림톡) |
| API 문서 | springdoc-openapi (Swagger UI, Scalar) |
| 빌드 | Gradle 8.14.3 |
| 테스트 | JUnit 5, H2, Testcontainers |
| 배포 | Docker Compose, GitHub Actions |

## 아키텍처

계층형 구조이며 의존 방향은 바깥에서 안쪽으로 단방향입니다.

```
web  →  application  →  domain  ←  infrastructure
```

| 계층 | 역할 |
|---|---|
| `web` | 컨트롤러, 요청·응답 DTO, 매퍼. HTTP 관심사만 다룹니다. |
| `application` | 서비스, 유스케이스, 아웃바운드 포트 인터페이스 |
| `domain` | JPA 엔티티, 도메인 규칙, 리포지토리 인터페이스 |
| `infrastructure` | JPA·Redis 구현체, 외부 연동(Google OAuth, Solapi, 파일 저장소) |
| `common` | 인증 필터, 공통 응답·예외, 설정 |

```
src/main/java/org/forif_backend/
├── web/              컨트롤러 21개, 엔드포인트 151개
├── application/
├── domain/
├── infrastructure/
└── common/
```

주요 도메인은 스터디, 수강 신청, 해커톤, 부원·운영진, 학기, 게시글, 부원 서비스입니다.

## 로컬 실행

### 사전 요구사항

- JDK 17
- MySQL 8.0 — `127.0.0.1:3307`에 `forif_dev` 스키마
- Redis 7 — `localhost:6379`
- Docker — `FlywayMigrationSchemaTest`가 Testcontainers로 MySQL을 띄웁니다

### 실행

```bash
SPRING_PROFILES_ACTIVE=local RDS_PASSWORD=<비밀번호> ./gradlew bootRun
```

**활성 프로파일을 반드시 지정해야 합니다.** 기본 프로파일이 설정돼 있지 않아 생략하면 데이터소스가 없어 부팅에 실패합니다.

저장소 루트에 `.env` 파일을 두면 `bootRun`이 자동으로 읽어 주입합니다(셸에 이미 있는 값이 우선). `java -jar`로 실행할 때는 적용되지 않습니다.

서버는 8080 포트로 뜹니다.

### 환경변수

`local` 프로파일에서 **`RDS_PASSWORD`만 필수**이고 나머지는 기본값이 있습니다.

| 변수 | 기본값(local) | 설명 |
|---|---|---|
| `RDS_URL` | `jdbc:mysql://127.0.0.1:3307` | **DB 이름과 끝 슬래시를 붙이지 않습니다.** 스키마명은 프로파일이 이어붙입니다 |
| `RDS_USERNAME` | `root` | |
| `RDS_PASSWORD` | 없음 | |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | `local` 프로파일에서만 쓰입니다 |
| `JWT_SECRET` | 로컬용 더미 값 | 운영에서는 반드시 주입 |
| `SMS_API_KEY` / `SMS_API_SECRET` / `SMS_PF_ID` / `SMS_SENDER_NUMBER` | 더미 값 | 카카오 알림톡 |
| `FILE_STORAGE` | `local` | 현재 구현체는 로컬 디스크뿐입니다 |
| `FILE_STORAGE_ROOT` | `./storage/uploads` | |
| `FILE_PUBLIC_URL` | 빈 문자열 | 파일 URL 앞에 붙는 공개 주소 |

### 프로파일

| 프로파일 | DB | `ddl-auto` | Flyway | 용도 |
|---|---|---|---|---|
| `local` | MySQL `forif_dev` | `update` | 꺼짐 | 로컬 개발 |
| `dev` | MySQL `forif_dev` | `update` | 꺼짐 | |
| `test` | H2 인메모리 | `create-drop` | 꺼짐 | 테스트 |
| `release` | MySQL `forif` | `validate` | **켜짐** | 운영 |

운영 프로파일 이름은 `prod`가 아니라 **`release`** 입니다.

## DB 마이그레이션

스키마는 Flyway로 관리하며 `src/main/resources/db/migration`에 둡니다. 현재 `V1__baseline_schema.sql`(운영 스키마 33테이블)이 기준선입니다.

- **`V1`은 수정하지 않습니다.** 모든 변경은 `V2` 이상을 새로 추가합니다.
- 운영은 Flyway로 스키마를 적용한 뒤 `ddl-auto: validate`가 엔티티와의 일치를 재확인합니다. 어긋나면 부팅에 실패합니다.
- `FlywayMigrationSchemaTest`가 CI에서 실제 MySQL 컨테이너에 마이그레이션을 적용하고 엔티티와 대조합니다. 나머지 테스트는 H2라 MySQL 전용 문법을 검증하지 못하므로, **이것이 배포 전 유일한 검증 지점**입니다.

`.gitignore`가 `*.sql`을 제외하지만 `db/migration`은 예외로 열려 있습니다.

## API 규약

- **응답 래퍼** — 모든 응답이 `ApiResponse<T>`(`timestamp`, `data`, `error_code`, `message`)로 감싸집니다. 예외는 파일 서빙(`GET /api/v1/files/**`)뿐입니다.
- **네이밍** — 요청·응답 바디는 `SNAKE_CASE`입니다. 쿼리 파라미터는 전역 전략의 영향을 받지 않아 이름이 혼재합니다.
- **시간대** — `Asia/Seoul`
- **인증** — Access Token은 `Authorization: Bearer` 헤더로, Refresh Token은 HttpOnly 쿠키로 전달합니다. 인증·인가 실패도 동일한 `ApiResponse` 포맷을 유지합니다.
- **페이지네이션** — `CursorPageResponse<T>`가 커서 방식과 페이지 방식을 겸합니다. `cursor`와 `page`를 모두 생략하면 커서 모드 첫 페이지입니다.
- **에러 코드** — `FOR{번호}-{HTTP상태}` 형식이며, `GET /api/v1/error-codes`가 전체 목록을 반환합니다.

### API 문서

- [Swagger UI](https://dev.forif.org/swagger-ui/index.html)
- Scalar: `/scalar`
- OpenAPI JSON: `/v3/api-docs`

## 브랜치 전략과 배포

```
feature/FOR-*  ──PR──▶  dev  ──PR──▶  release  ──PR──▶  main
                                          │
                                          └─ push 시 자동 배포
```

- 기능 개발은 `dev`에서 `feature/FOR-*` 브랜치를 따서 진행하고 PR로 `dev`에 머지합니다.
- 릴리즈는 `dev` → `release` PR로 올리고, 머지되면 **자동 배포**가 실행됩니다.
- 배포가 확인되면 `release` → `main` PR로 반영하고 태그를 답니다.

### 자동 배포

`release` 브랜치 push(또는 수동 실행)에서만 동작합니다.

1. 테스트를 포함한 빌드 — 마이그레이션 검증 테스트가 여기서 걸러집니다
2. JAR 전송 후 체크섬 검증
3. 기존 JAR 백업 → 교체 → 컨테이너 재생성
4. 헬스체크(`/api/v1/semesters/current`) 최대 180초
5. **실패하면 직전 JAR로 자동 롤백**하고, 롤백 후 서비스가 살아났는지까지 확인합니다

> 자동 롤백은 JAR에 한정됩니다. Flyway가 스키마를 바꾼 뒤 실패한 경우 DB는 되돌아가지 않으므로 `flyway_schema_history`를 직접 확인해야 합니다. 워크플로가 이 상황을 경고로 남깁니다.

필요한 GitHub Secrets는 `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY` 세 개입니다. DB 비밀번호 등 나머지 값은 서버의 `.env`에서 관리하며 CI는 알지 못합니다.

## 기여자

[Contributors](https://github.com/forif-npo/forif-backend/graphs/contributors)

## 라이선스

MIT License

Copyright (c) 2024 Byeonghyun Yang

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## 문의

- 학회: forif.contact@gmail.com
- 웹사이트: [forif.org](https://forif.org)
