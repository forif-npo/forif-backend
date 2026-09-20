# 데이터베이스와 마이그레이션

## 구성

| 항목 | 값 |
|---|---|
| DBMS | MySQL 8.0 |
| 운영 스키마 | `forif` |
| 문자셋 | `utf8mb4` |
| 테이블 | 36개 (`flyway_schema_history` 포함) |
| 스키마 관리 | Flyway |

## 두 가지 안전장치

운영은 스키마를 **두 겹으로** 지킵니다.

```
Flyway 가 마이그레이션 적용  ──▶  Hibernate 가 ddl-auto: validate 로 엔티티와 대조
```

Flyway 가 스키마를 만들고, 그 결과가 JPA 엔티티와 일치하는지 Hibernate 가 다시 확인합니다. **어긋나면 애플리케이션이 부팅되지 않습니다.**

이 구조 때문에 **엔티티에 필드를 추가하면 마이그레이션도 반드시 함께 추가**해야 합니다. 빠뜨리면 운영 배포 시점에 부팅이 실패합니다.

## 프로파일별 설정

| 프로파일 | DB | `ddl-auto` | Flyway |
|---|---|---|---|
| `local` | MySQL `forif_dev` | `validate` | 켜짐 |
| `dev` | MySQL `forif_dev` | `validate` | 켜짐 |
| `test` | H2 인메모리 | `create-drop` | 꺼짐 |
| `release` | MySQL `forif` | `validate` | 켜짐 |

`application.yml` 의 공통 기본값은 **`spring.flyway.enabled: false`** 입니다. 프로파일을 지정하지 않는 `@DataJpaTest` 가 이 기본값을 쓰기 때문입니다. H2 에 MySQL 전용 베이스라인을 실행하려다 실패하는 것을 막습니다.

실제 MySQL 을 쓰는 프로파일에서만 명시적으로 켭니다.

## 마이그레이션 파일

`src/main/resources/db/migration/` 에 있습니다.

| 파일 | 내용 |
|---|---|
| `V1__baseline_schema.sql` | Flyway 도입 시점의 운영 스키마 (33테이블) |
| `V2__add_registration_withdrawn_to_member_semester_check.sql` | 등록 철회 플래그 |
| `V3__enforce_normalized_user_phone_numbers.sql` | 전화번호 정규화 + CHECK·UNIQUE 제약 |
| `V4__add_colleges_departments_and_user_department_id.sql` | 단과대학·학과 카탈로그와 부원 학과 FK |

### V1 은 특별합니다

V1 은 Flyway 도입 당시 운영 스키마를 그대로 덤프한 것입니다.

- **이미 스키마가 있던 DB 는 V1 을 실행하지 않습니다.** `baseline-on-migrate: true` 가 "이미 V1 이다"라고 이력만 기록하고 V2 부터 적용합니다.
- **빈 DB 에서는 실제로 실행되어** 동일한 스키마를 만듭니다.
- **V1 은 절대 수정하지 마세요.** 베이스라인된 DB 는 체크섬을 검증하지 않고 넘어가지만, 실제로 V1 을 실행한 환경(신규 DB, CI)에서만 체크섬 불일치로 터져 환경마다 동작이 갈립니다.

`.gitignore` 가 `*.sql` 을 제외하지만 `db/migration` 은 예외로 열려 있습니다. 예외가 없으면 마이그레이션 파일이 커밋되지 않습니다.

## 마이그레이션 작성 규칙

### 반드시 지킬 것

**MySQL 은 DDL 이 트랜잭션이 아닙니다.** 스크립트 중간에서 실패하면 앞쪽 DDL 은 커밋된 채 남고, `flyway_schema_history` 에 `success=0` 행이 기록됩니다. 그러면 **이후 모든 부팅이 "Detected failed migration" 으로 차단**되며, 운영 DB 에 직접 접속해 부분 적용분을 정리하고 `flyway repair` 를 돌려야 풀립니다.

그래서 아래를 지켜야 합니다.

1. **운영 데이터 복제본에 먼저 돌려보세요.** 빈 DB 에서 통과하는 것과 운영 데이터에서 통과하는 것은 다릅니다. 방법은 아래 "리허설" 절에 있습니다.
2. **제약을 추가하기 전에 기존 데이터를 먼저 정리하세요.** `NOT NULL`, `UNIQUE`, `CHECK` 는 기존 행을 검증합니다. 위반 행이 하나라도 있으면 `ALTER` 가 실패합니다.
3. **버전 번호가 겹치지 않는지 확인하세요.** 여러 PR 이 동시에 열려 있으면 같은 번호를 쓰기 쉽습니다.

### 실제로 겪은 사례

V3 의 초안은 전화번호를 정규화한 뒤 `CHECK (phone_num REGEXP '^[0-9]+$')` 를 걸었습니다. 그런데 운영에 전화번호가 `-` 한 글자인 행이 하나 있었고, 정규화하면 빈 문자열이 되어 CHECK 를 위반했습니다.

```
ERROR 3819 (HY000): Check constraint 'chk_tb_user_phone_num_normalized' is violated.
```

**`UPDATE` 는 이미 커밋된 뒤였습니다.** 데이터는 바뀌고 제약은 안 걸린 상태로 남았습니다. 리허설로 사전에 잡았고, 빈 문자열이 되는 값을 `NULL` 로 보내는 것으로 고쳤습니다.

```sql
SET phone_num = NULLIF(CASE ... END, '')
```

### 리허설 방법

운영 서버에서 스키마를 복제해 마이그레이션만 돌려봅니다.

```bash
ssh forif-dev
source /srv/forif/app/.env
P="$MYSQL_ROOT_PASSWORD"

# 복제 (필요한 테이블만 떠도 됩니다)
docker exec forif-mysql mysql -uroot -p"$P" -e \
  "DROP DATABASE IF EXISTS forif_rehearsal; CREATE DATABASE forif_rehearsal CHARACTER SET utf8mb4;"
docker exec forif-mysql sh -c \
  "mysqldump -uroot -p'$P' --no-tablespaces forif | mysql -uroot -p'$P' forif_rehearsal"

# 마이그레이션 실행
docker cp ./V5__xxx.sql forif-mysql:/tmp/V5.sql
docker exec forif-mysql sh -c "mysql -uroot -p'$P' forif_rehearsal < /tmp/V5.sql"

# 정리
docker exec forif-mysql mysql -uroot -p"$P" -e "DROP DATABASE forif_rehearsal;"
```

더 확실하게 하려면 **PR 브랜치 JAR 을 빌드해 복제본에 부팅**해보세요. Flyway 적용과 `ddl-auto: validate` 를 한 번에 검증합니다.

```bash
./gradlew bootJar -x test
scp build/libs/*-SNAPSHOT.jar forif-dev:/tmp/pr.jar
# 서버에서 eclipse-temurin:17-jre 컨테이너로 복제본을 가리켜 실행
```

### 자동 검증

`FlywayMigrationSchemaTest` 가 Testcontainers 로 실제 MySQL 8.0.46 을 띄우고, 빈 DB 에 마이그레이션을 전부 적용한 뒤 `ddl-auto: validate` 로 엔티티와 대조합니다. 컨텍스트 로딩 자체가 검증이라 테스트 본문은 비어 있습니다.

**나머지 테스트는 H2 라 MySQL 전용 문법을 검증하지 못합니다.** 이 테스트가 CI 에서 마이그레이션을 실제로 실행하는 유일한 지점이므로 삭제하거나 비활성화하지 마세요. Docker 가 필요합니다.

`PhoneNumberMigrationTest` · `PhoneNumberConstraintTest` 도 같은 방식으로 V3 의 데이터 변환과 제약을 검증합니다.

## 운영 이력 확인

```bash
ssh forif-dev
source /srv/forif/app/.env
docker exec forif-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" forif -e \
  "SELECT version, description, type, success, installed_on FROM flyway_schema_history ORDER BY installed_rank;"
```

`success=0` 행이 보이면 그 마이그레이션이 중간에 실패한 것입니다. 부분 적용분을 수동으로 되돌린 뒤 `flyway repair` 로 이력을 정리해야 다음 배포가 가능합니다.

## 알아둘 스키마 특성

**`tb_study.study_id = 0` 인 행이 있습니다.** 2026-1 자율스터디가 auto_increment 테이블에 `0` 으로 들어가 있고 수강 관계 39건이 여기 달려 있습니다. `study_id > 0` 같은 조건을 쓰면 이 39명이 통째로 사라집니다. 2026-2 자율스터디는 정상 채번(`125`)됐습니다.

**`tb_user.phone_num` 에 `UNIQUE` 가 걸려 있습니다** (V3). 값이 없는 경우는 `NULL` 이며, `CHECK` 가 `NULL` 을 허용하고 MySQL `UNIQUE` 도 `NULL` 은 중복으로 보지 않습니다.

**`tb_user_apply.primary_study` · `secondary_study` 에 FK 가 있습니다.** 존재하지 않는 스터디를 가리킬 수 없습니다.

**`tb_mentor_study` 는 레거시입니다.** 현재 코드는 쓰지 않고 과거 데이터만 남아 있습니다.

## 후속 과제

**`baseline-on-migrate` 를 `false` 로 되돌려야 합니다.** 운영에 베이스라인 기록이 끝났으므로 역할이 끝났습니다. 켜둔 채로 두면 `RDS_URL` 오타 등으로 엉뚱한 비어있지 않은 DB 를 가리켜도 "이미 v1" 이라고 승인하고 넘어갑니다. Flyway 가 이상을 잡아줄 수 있었던 지점을 오히려 덮습니다.
