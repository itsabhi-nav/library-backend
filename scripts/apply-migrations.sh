#!/usr/bin/env bash
# Apply Flyway migrations (V1–V6) by starting Spring Boot once, then stopping.
# Usage: copy .env.example to .env, set DATABASE_PASSWORD, then: ./scripts/apply-migrations.sh

set -euo pipefail
cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  echo "Missing .env — copy .env.example to .env and set DATABASE_PASSWORD from Neon."
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

if [[ -z "${DATABASE_PASSWORD:-}" ]]; then
  echo "DATABASE_PASSWORD is empty in .env"
  exit 1
fi

echo "Starting backend to run Flyway migrations…"
./mvnw -q spring-boot:run -Dspring-boot.run.arguments=--spring.main.web-application-type=none &
PID=$!

for i in {1..90}; do
  if grep -q "Started LibraryBackendApplication" /tmp/library-migrate.log 2>/dev/null; then
    break
  fi
  sleep 2
done

kill "$PID" 2>/dev/null || true
wait "$PID" 2>/dev/null || true

echo "Done. Restart backend normally: ./mvnw spring-boot:run"
