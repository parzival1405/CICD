# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -ntp dependency:go-offline

COPY src src
RUN ./mvnw -B -ntp package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --no-create-home app

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
