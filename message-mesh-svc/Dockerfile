# syntax=docker/dockerfile:1

# ---- Build stage: compile and package the Spring Boot fat JAR ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies first (only re-download when pom.xml changes).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Compile and package (tests are run in CI, skipped here for faster images).
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage: slim JRE with a non-root user ----
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Run as an unprivileged user.
RUN groupadd --system app && useradd --system --gid app --home /app app

# Copy the built artifact (artifactId-version.jar).
COPY --from=build /build/target/message-mesh-svc-*.jar app.jar

# Writable directory for the H2 file DB when running the dev profile.
RUN mkdir -p /app/data && chown -R app:app /app
USER app

ENV JAVA_OPTS="" \
    SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD ["sh", "-c", "wget --quiet --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1"]

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
