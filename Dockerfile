# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp package -DskipTests

# ---------- Stage 2: tách layer ----------
FROM eclipse-temurin:21-jre AS extract
WORKDIR /extract
COPY --from=build /workspace/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers

# ---------- Stage 3: runtime ----------
FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home app    

WORKDIR /app
COPY --from=extract /extract/layers/dependencies/ ./
COPY --from=extract /extract/layers/spring-boot-loader/ ./
COPY --from=extract /extract/layers/snapshot-dependencies/ ./
COPY --from=extract /extract/layers/application/ ./

USER 10001:10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
