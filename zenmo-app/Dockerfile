# syntax=docker/dockerfile:1

# Backend image for zenmo-app (Spring Boot).
#
# Build context must be the repository root so Maven can resolve the parent POM
# (zenmo-parent) and checkstyle.xml next to it:
#   docker build -f zenmo-app/Dockerfile -t backend .
#
# If a Dockerfile symlink exists at the repository root, you can use:
#   docker build -t backend .

# -----------------------------------------------------------------------------
# Stage: build — Maven + Eclipse Temurin JDK 25 compiles and packages the fat JAR.
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build

# Layer cache: resolve dependencies from POMs only before copying sources, so code
# changes do not invalidate downloaded dependencies.
COPY pom.xml checkstyle.xml ./
COPY zenmo-app/pom.xml zenmo-app/

RUN mvn -f zenmo-app/pom.xml -B dependency:go-offline -DskipTests

COPY zenmo-app/src zenmo-app/src

RUN mvn -f zenmo-app/pom.xml -B clean package -DskipTests

# -----------------------------------------------------------------------------
# Stage: runtime — JRE-only image with the executable JAR (no Maven, no sources).
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-jammy AS runtime

WORKDIR /app

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=build /build/zenmo-app/target/zenmo.jar app.jar

RUN mkdir -p /app/logs && chown app:app /app/logs app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
