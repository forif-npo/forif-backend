# 배포와 운영

## 서버 구성

단일 리눅스 서버(Ubuntu 22.04)에서 Docker Compose 로 컨테이너 세 개를 띄웁니다. AWS 를 쓰지 않습니다.

| 컨테이너 | 이미지 | 역할 |
|---|---|---|
| `forif-backend-dev` | `eclipse-temurin:17-jre` | 애플리케이션 |
| `forif-mysql` | `mysql:8.0` | 데이터베이스 |
| `forif-redis` | `redis:7-alpine` | 토큰 저장소 |

### 애플리케이션 컨테이너

**이미지를 빌드하지 않습니다.** JRE 이미지에 JAR 을 바인드 마운트해 실행합니다.

| 항목 | 값 |
|---|---|
| Compose 파일 | `/srv/forif/app/docker-compose.yml` |
| JAR | `/srv/forif/app/forif-backend-dev.jar` → `/app/app.jar` (읽기 전용) |
| 업로드 | `/srv/forif/uploads` → `/app/uploads` |
| 실행 | `java -jar /app/app.jar` |
| 포트 | `127.0.0.1:8080` (리버스 프록시 뒤) |
| 프로파일 | `release` |
| 재시작 | `unless-stopped` |

**JAR 은 root 소유이고 바인드 마운트입니다.** 교체할 때 두 가지를 지켜야 합니다.

- `sudo` 가 필요합니다.
- **`mv` 가 아니라 `cp` 를 써야 합니다.** `mv` 는 inode 가 바뀌어 컨테이너가 옛 파일을 계속 봅니다.

### 서버 환경변수

`/srv/forif/app/.env` 에 있습니다. **GitHub Secrets 가 아닙니다.**

```
MYSQL_ROOT_PASSWORD  MYSQL_USER  MYSQL_PASSWORD
JWT_SECRET
SMS_API_KEY  SMS_API_SECRET  SMS_PF_ID  SMS_SENDER_NUMBER
FILE_STORAGE  FILE_STORAGE_ROOT  FILE_STORAGE_HOST_PATH  FILE_PUBLIC_URL
```

DB 접속 정보는 compose 가 `RDS_URL=jdbc:mysql://mysql:3306`, `RDS_USERNAME=${MYSQL_USER}`, `RDS_PASSWORD=${MYSQL_PASSWORD}` 로 주입합니다. `RDS_*` 라는 이름은 AWS 시절의 잔재이고 실제로는 같은 서버의 MySQL 컨테이너입니다.

## 자동배포

`.github/workflows/deploy.yml` 하나가 CI 와 배포를 모두 담당합니다.

| 잡 | 조건 |
|---|---|
| `build-test` | `main` · `release` · `dev` · `feature/**` push, 그리고 PR |
| `deploy` | **`release` 브랜치 push 또는 수동 실행일 때만** |

`deploy` 잡에만 `concurrency: deploy-production` 이 걸려 있어 배포는 직렬화됩니다. 워크플로 전역에 걸면 모든 브랜치 CI 가 한 줄로 서기 때문에 잡 단위로 둔 것입니다.

### 절차

```
1. 빌드·테스트          마이그레이션 검증 테스트가 여기서 걸러진다
2. JAR scp 전송         전송 후 sha256 체크섬 검증
3. 기존 JAR 백업        forif-backend-dev.jar.bak-YYYYmmdd-HHMMSS
4. JAR 교체             sudo cp (mv 아님), 교체 후 체크섬 재검증
5. 컨테이너 재생성       docker compose up -d --force-recreate backend
6. 헬스체크             최대 180초, 5초 간격
7. 백업 정리            최신 5개만 유지
```

**어느 단계에서 실패하든 직전 JAR 로 자동 롤백**하고, 롤백 후 서비스가 실제로 살아났는지까지 확인해 구분해서 보고합니다.

| 상황 | 결과 |
|---|---|
| 헬스체크 통과 | 성공, 오래된 백업 정리 |
| 실패 후 롤백 성공 | `배포 실패. 이전 버전으로 롤백했고 서비스는 정상이다.` |
| 실패 후 롤백도 실패 | `수동 확인이 필요하다.` |

부팅 실패가 로그에 이미 찍혔으면 180초를 채우지 않고 즉시 롤백합니다.

### 헬스체크

`GET http://127.0.0.1:8080/api/v1/semesters/current` 를 씁니다.

actuator 를 쓰지 않기 때문입니다. 이 경로는 인증 없이 열려 있으면서 DB 까지 거치므로 연결성까지 확인됩니다.

### 필요한 Secrets

| 이름 | 값 |
|---|---|
| `EC2_HOST` | 서버 IP |
| `EC2_USER` | SSH 사용자 |
| `EC2_SSH_KEY` | CI 전용 SSH 개인키 |

세 개뿐입니다. 배포 잡 첫 단계가 누락 여부를 검사하고 서버에 접속하기 전에 실패시킵니다.

**DB 비밀번호나 JWT 시크릿은 Secrets 에 없습니다.** 서버 `.env` 소관이고 CI 는 알 필요가 없습니다. 이 편이 유출 범위가 좁아 안전합니다.

CI 전용 키는 개인 키와 분리해 발급하고 공개키만 서버 `authorized_keys` 에 등록합니다.

## 자동 롤백의 한계

**JAR 만 되돌립니다. DB 는 되돌아가지 않습니다.**

Flyway 가 스키마를 바꾼 뒤 부팅이 실패하면 JAR 롤백으로 서비스는 살아나지만 스키마는 이미 변경된 상태입니다. 워크플로가 이 상황을 감지하면 경고를 남깁니다.

> 로그에 Flyway 출력이 있다. DB 스키마가 이미 변경됐을 수 있고 JAR 롤백으로는 되돌아가지 않는다. `flyway_schema_history` 를 직접 확인할 것.

이 경고가 뜨면 [database.md](database.md#운영-이력-확인) 의 절차로 이력을 확인하세요.

## 브랜치와 릴리즈

```
feature/FOR-*  ──PR──▶  dev  ──PR──▶  release  ──PR──▶  main
                                         │
                                         └─ push 시 자동배포
```

1. `dev` 에서 `feature/FOR-*` 를 따서 작업하고 PR 로 `dev` 에 머지합니다.
2. 릴리즈는 `dev` → `release` PR 로 올리고, 머지되면 **자동배포**가 실행됩니다.
3. 배포가 확인되면 `release` → `main` PR 로 반영하고 태그를 답니다.

`main` 과 `release` 둘 다 영구 브랜치입니다. **머지된 피처 브랜치는 삭제하지 않습니다.**

## 운영 점검

```bash
ssh forif-dev

# 컨테이너 상태
docker ps

# 헬스체크
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/api/v1/semesters/current

# 최근 로그
docker logs --tail 100 forif-backend-dev

# 마이그레이션 이력
source /srv/forif/app/.env
docker exec forif-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" forif -e \
  "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

# 디스크·백업
df -h /srv
ls -1 /srv/forif/app/forif-backend-dev.jar.bak-* | wc -l
```

### 수동 롤백

자동 롤백이 실패했을 때입니다.

```bash
ssh forif-dev
cd /srv/forif/app
ls -1t forif-backend-dev.jar.bak-*        # 되돌릴 백업 고르기
sudo cp forif-backend-dev.jar.bak-<타임스탬프> forif-backend-dev.jar
docker compose up -d --force-recreate backend
```

**`cp` 를 쓰세요.** `mv` 는 inode 가 바뀌어 컨테이너가 옛 파일을 계속 봅니다.
