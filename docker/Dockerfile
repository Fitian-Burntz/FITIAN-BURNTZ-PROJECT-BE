# ---- Build stage ------------------------------------------------------------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Gradle Wrapper 권한 문제 예방(윈도우 → 리눅스)
COPY gradlew ./
RUN chmod +x gradlew

# 의존성/설정 먼저 복사 → 캐시효율↑
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
# 소스 복사
COPY src ./src

# 테스트는 속도 위해 스킵(필요시 -x test 제거)
RUN ./gradlew clean bootJar -x test

# 실행 가능한 bootJar만 app.jar로 이름 고정(plain.jar 제외)
RUN set -eux; \
    JAR_PATH="$(find /app/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)"; \
    test -n "$JAR_PATH"; \
    cp "$JAR_PATH" /app/app.jar

# ---- Runtime stage ----------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 컨테이너 메모리 한도 반영 & 힙 상한 퍼센트 설정
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

# 빌드 산출물만 가져오기 → 런타임 이미지 슬림
COPY --from=build /app/app.jar ./app.jar

# 앱 포트
EXPOSE 8080

# 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]