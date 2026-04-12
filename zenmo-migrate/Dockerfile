# syntax=docker/dockerfile:1

# Flyway migration runner (shaded JAR). One-shot: applies migrations and exits.
#
# Build context must be the repository root (zenmo-parent + zenmo-migrate module):
#   docker build -f zenmo-migrate/Dockerfile -t zenmo-migrate .
#
# If Dockerfile.migrate symlink exists at the repository root:
#   docker build -f Dockerfile.migrate -t zenmo-migrate .
#
# Run (example — set credentials for your environment):
#   docker run --rm \
#     -e ZENMO_DATASOURCE_URL=jdbc:postgresql://db:5432/zenmo \
#     -e ZENMO_DATASOURCE_USERNAME=zenmo \
#     -e ZENMO_DATASOURCE_PASSWORD=secret \
#     zenmo-migrate

# -----------------------------------------------------------------------------
# Stage: build — Maven + Eclipse Temurin JDK 25 packages the shaded executable JAR.
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build

COPY pom.xml ./
COPY zenmo-migrate/pom.xml zenmo-migrate/

RUN mvn -f zenmo-migrate/pom.xml -B dependency:go-offline -DskipTests

COPY zenmo-migrate/src zenmo-migrate/src

RUN mvn -f zenmo-migrate/pom.xml -B clean package -DskipTests

# -----------------------------------------------------------------------------
# Stage: runtime — JRE 25 only; runs Flyway then exits (no long-lived process).
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=build /build/zenmo-migrate/target/zenmo-migrate.jar migrate.jar

RUN chown app:app migrate.jar

USER app

ENTRYPOINT ["java", "-jar", "migrate.jar"]
