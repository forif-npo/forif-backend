# 개발 환경

## 사전 요구사항

| 항목 | 비고 |
|---|---|
| JDK 17 | |
| MySQL 8.0 | `127.0.0.1:3307` 에 `forif_dev` 스키마 |
| Redis 7 | `localhost:6379` |
| Docker | 마이그레이션 검증 테스트가 Testcontainers 로 MySQL 을 띄웁니다 |

MySQL 포트가 **3307** 입니다. 표준 3306 이 아닙니다.

## 실행

```bash
SPRING_PROFILES_ACTIVE=local RDS_PASSWORD=<비밀번호> ./gradlew bootRun
```

**활성 프로파일을 반드시 지정해야 합니다.** 기본 프로파일이 없어서 생략하면 데이터소스 설정이 없어 부팅에 실패합니다.

저장소 루트에 `.env` 파일을 두면 `bootRun` 이 자동으로 읽어 주입합니다. 셸에 이미 있는 값이 우선하고, `java -jar` 로 실행할 때는 적용되지 않습니다.

서버는 8080 포트로 뜹니다.

## 환경변수

`local` 프로파일에서 **`RDS_PASSWORD` 만 필수**이고 나머지는 기본값이 있습니다.

| 변수 | 기본값 (local) | 설명 |
|---|---|---|
| `RDS_URL` | `jdbc:mysql://127.0.0.1:3307` | **DB 이름과 끝 슬래시를 붙이지 않습니다.** 스키마명은 프로파일이 이어붙입니다 |
| `RDS_USERNAME` | `root` | |
| `RDS_PASSWORD` | 없음 | |
| `REDIS_HOST` · `REDIS_PORT` | `localhost` · `6379` | `local` 프로파일 전용 |
| `JWT_SECRET` | 로컬용 더미 값 | BASE64 문자열 |
| `SMS_API_KEY` · `SMS_API_SECRET` · `SMS_PF_ID` · `SMS_SENDER_NUMBER` | 더미 값 | 카카오 알림톡 |
| `FILE_STORAGE` | `local` | 현재 구현체는 로컬 디스크뿐 |
| `FILE_STORAGE_ROOT` | `./storage/uploads` | |
| `FILE_PUBLIC_URL` | 빈 문자열 | 파일 URL 앞에 붙는 공개 주소 |

`RDS_URL` 에 DB 이름을 붙이면 `jdbc:mysql://host:3307/forif_dev/forif_dev` 가 되어 접속에 실패합니다.

## 로컬 DB 는 Flyway 가 관리합니다

`local` · `dev` 프로파일은 **`ddl-auto: validate` + Flyway 켜짐** 입니다. Hibernate 가 스키마를 만들어주지 않습니다.

기존에 `ddl-auto: update` 로 만들어 쓰던 `forif_dev` 가 있다면 **지우고 다시 만드세요.**

```sql
DROP DATABASE forif_dev;
CREATE DATABASE forif_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

빈 스키마에서 시작하면 Flyway 가 V1 부터 순서대로 적용해 운영과 동일한 스키마를 만듭니다.

기존 DB 를 그대로 쓰면 이런 일이 생깁니다.

- `baseline-on-migrate` 가 "이미 V1 이다"라고 도장만 찍고 V2 부터 적용하는데, 실제 스키마가 V1 과 다르면 어긋난 상태 위에서 돌아갑니다.
- V3 가 `tb_user.phone_num` 에 `UNIQUE` 를 겁니다. 더미 데이터에 중복 번호가 있으면 마이그레이션이 실패하고, **MySQL 은 DDL 을 롤백하지 않으므로 이후 모든 부팅이 막힙니다.** 직접 정리하고 `flyway repair` 를 돌려야 풀립니다.

## 테스트

```bash
./gradlew test
```

테스트 클래스 63개이며 구성은 이렇습니다.

| 종류 | 환경 | 비고 |
|---|---|---|
| 대부분 | H2 인메모리 + `create-drop` | Flyway 꺼짐 |
| 마이그레이션 검증 3개 | Testcontainers MySQL 8.0.46 | **Docker 필요** |

`@Disabled` 로 꺼둔 테스트가 몇 개 있습니다. 실제 Google API 호출이 필요하거나 로컬 Redis 가 있어야 하는 통합 테스트입니다.

### 테스트 작성 시 주의

**`@DataJpaTest` 는 프로파일을 지정하지 않으면 `application.yml` 의 공통 설정을 씁니다.** `application-test.yml` 에 넣은 설정은 적용되지 않습니다. Flyway 기본값을 꺼둔 이유가 이것입니다.

**Mockito 는 인터페이스의 `default` 메서드를 가로채 `null` 을 반환합니다.** 포트 인터페이스에 `default` 메서드를 추가하면 그 인터페이스를 목으로 쓰는 기존 테스트가 조용히 깨집니다.

**테스트가 실제로 무언가를 검증하는지 확인하세요.** 후보가 하나뿐인 목록에 필터를 걸고 그 하나가 나오는지 보는 테스트는, 필터를 통째로 지워도 통과합니다. 걸러져야 할 대조군을 함께 넣으세요.

### 마이그레이션 검증 테스트

`FlywayMigrationSchemaTest` 는 빈 MySQL 컨테이너에 마이그레이션을 전부 적용한 뒤 `ddl-auto: validate` 로 엔티티와 대조합니다. 컨텍스트 로딩 자체가 검증이라 본문이 비어 있습니다.

**이 테스트가 CI 에서 마이그레이션을 실제 MySQL 에 실행하는 유일한 지점입니다.** 다른 테스트는 모두 H2 라 MySQL 전용 문법을 검증하지 못합니다. 삭제하거나 비활성화하지 마세요.

## 브랜치와 커밋

### 브랜치

`dev` 에서 `feature/FOR-<이슈번호>` 를 따서 작업하고 PR 로 `dev` 에 머지합니다. 자세한 흐름은 [deployment.md](deployment.md#브랜치와-릴리즈) 를 보세요.

**머지된 피처 브랜치는 삭제하지 않습니다.** `gh pr merge` 에 `--delete-branch` 를 쓰지 마세요.

브랜치를 만들기 전에 같은 이름이 원격에 있는지 확인하세요.

```bash
git ls-remote --heads origin 'feature/FOR-*'
```

### 커밋과 PR 제목

`type(scope): 설명` 형식입니다.

```
feat(admin): 합격자 등록 철회 처리 추가
fix(user): 문자 발송 서비스를 위한 전화번호 정규화
chore: Flyway 도입으로 DB 스키마 변경 이력 관리
docs: README 를 실제 구성에 맞게 다시 작성
```

**PR 제목이 `Feature/for 172` 처럼 자동 생성된 채로 남지 않게 하세요.** GitHub 이 브랜치명에서 만든 값이라 목록에서 변경 내용을 알 수 없습니다.

### PR 을 올리기 전에

- `./gradlew build` 가 통과하는지 확인합니다.
- 엔티티를 바꿨으면 마이그레이션이 함께 있는지 확인합니다.
- 마이그레이션을 추가했으면 **운영 데이터 복제본에 리허설**합니다 ([database.md](database.md#리허설-방법)).
- 기본 브랜치가 `main` 이므로 **`--base dev` 를 명시**합니다.

```bash
gh pr create --base dev --head feature/FOR-000 --title "..." --body "..."
```

## 코드 스타일

- 주석은 한국어로, **왜 그렇게 했는지**를 적습니다. 코드를 읽으면 아는 내용은 적지 않습니다.
- Swagger `@Tag` · `@Operation` 도 한국어입니다.
- 새 코드는 주변 코드의 관습을 따릅니다. 계층 규칙은 [architecture.md](architecture.md#계층-구조) 를 보세요.
