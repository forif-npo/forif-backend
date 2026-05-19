# 1단계: 빌드 스테이지
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 네트워크 문제 방지용 설정
ENV GRADLE_OPTS="-Dhttps.protocols=TLSv1.2,TLSv1.3"

# Gradle 래퍼와 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 의존성만 미리 받아두기
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 후 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon

# 2단계: 실행 스테이지
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Runtime profile is supplied by SPRING_PROFILES_ACTIVE in the compose/env file.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
