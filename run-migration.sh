#!/bin/bash
# -------------------------------------------------------------
# Run Flyway migrations via zenmo-migrate shaded JAR (foreground).
#
# Optional environment (defaults match zenmo-app dev config):
#   ZENMO_DATASOURCE_URL       default: jdbc:postgresql://localhost:5432/zenmo
#   ZENMO_DATASOURCE_USERNAME  default: zenmo
#   ZENMO_DATASOURCE_PASSWORD  default: zenmo
#
# Or JVM system properties: zenmo.datasource.url, .username, .password
# Example:
#   ZENMO_DATASOURCE_URL=jdbc:postgresql://db:5432/zenmo ./run-migration.sh
# -------------------------------------------------------------

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="${SCRIPT_DIR}/zenmo-migrate/target/zenmo-migrate.jar"

if [[ ! -f "$JAR_FILE" ]]; then
  echo "Missing: $JAR_FILE"
  echo "Build it first: mvn -pl zenmo-migrate -am package -DskipTests"
  exit 1
fi

exec java -jar "$JAR_FILE" "$@"
