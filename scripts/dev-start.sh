#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

docker compose up -d postgres

BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-3000}"

cleanup() {
  local code=$?
  trap - EXIT INT TERM
  if [[ -n "${BACKEND_PID:-}" ]]; then
    kill "${BACKEND_PID}" 2>/dev/null || true
  fi
  if [[ -n "${FRONTEND_PID:-}" ]]; then
    kill "${FRONTEND_PID}" 2>/dev/null || true
  fi
  wait "${BACKEND_PID:-}" "${FRONTEND_PID:-}" 2>/dev/null || true
  exit "${code}"
}

trap cleanup EXIT INT TERM

echo "Starting backend on http://localhost:${BACKEND_PORT}"
(
  cd backend
  DB_PASSWORD="${DB_PASSWORD:-changeme}" SERVER_PORT="${BACKEND_PORT}" ./gradlew bootRun
) &
BACKEND_PID=$!

echo "Starting frontend on http://localhost:${FRONTEND_PORT}"
(
  cd frontend
  PORT="${FRONTEND_PORT}" npm run dev
) &
FRONTEND_PID=$!

while true; do
  if ! kill -0 "${BACKEND_PID}" 2>/dev/null; then
    wait "${BACKEND_PID}"
    break
  fi
  if ! kill -0 "${FRONTEND_PID}" 2>/dev/null; then
    wait "${FRONTEND_PID}"
    break
  fi
  sleep 1
done
