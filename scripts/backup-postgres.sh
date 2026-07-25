#!/usr/bin/env bash
# Daily PostgreSQL backup for the Kuky docker-compose stack.
# Keeps the 10 most recent dumps (rolling retention).
#
# Install (on the VM, once):
#   chmod +x scripts/backup-postgres.sh
#   crontab -e
#   # Run every day at 03:00 (adjust path):
#   0 3 * * * /path/to/kuky/scripts/backup-postgres.sh >> /var/log/kuky-backup.log 2>&1
#
# Restore example:
#   gunzip -c backups/kuky_2026-07-25.sql.gz \
#     | docker compose exec -T postgres psql -U kuky -d kuky
#
# Override with env vars if needed:
#   BACKUP_DIR, RETENTION_COUNT, COMPOSE_FILE, POSTGRES_SERVICE,
#   POSTGRES_USER, POSTGRES_DB

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

BACKUP_DIR="${BACKUP_DIR:-${PROJECT_DIR}/backups}"
RETENTION_COUNT="${RETENTION_COUNT:-10}"
COMPOSE_FILE="${COMPOSE_FILE:-${PROJECT_DIR}/docker-compose.yml}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgres}"
POSTGRES_USER="${POSTGRES_USER:-kuky}"
POSTGRES_DB="${POSTGRES_DB:-kuky}"

TIMESTAMP="$(date +%Y-%m-%d)"
DUMP_FILE="${BACKUP_DIR}/kuky_${TIMESTAMP}.sql.gz"
TMP_FILE="${DUMP_FILE}.tmp"

cd "${PROJECT_DIR}"

if ! docker compose -f "${COMPOSE_FILE}" exec -T "${POSTGRES_SERVICE}" \
  pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" >/dev/null 2>&1; then
  echo "error: postgres service '${POSTGRES_SERVICE}' is not ready" >&2
  exit 1
fi

mkdir -p "${BACKUP_DIR}"

echo "$(date -Is) starting dump → ${DUMP_FILE}"

docker compose -f "${COMPOSE_FILE}" exec -T "${POSTGRES_SERVICE}" \
  pg_dump -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" --clean --if-exists \
  | gzip -c > "${TMP_FILE}"

# Refuse empty / tiny dumps (gzip header alone is a few bytes)
MIN_BYTES=100
SIZE="$(wc -c < "${TMP_FILE}" | tr -d ' ')"
if [[ "${SIZE}" -lt "${MIN_BYTES}" ]]; then
  rm -f "${TMP_FILE}"
  echo "error: dump is empty or too small (${SIZE} bytes)" >&2
  exit 1
fi

mv -f "${TMP_FILE}" "${DUMP_FILE}"
echo "$(date -Is) dump complete (${SIZE} bytes)"

# Keep only the newest RETENTION_COUNT dumps
mapfile -t OLD_DUMPS < <(
  ls -1t "${BACKUP_DIR}"/kuky_*.sql.gz 2>/dev/null | tail -n "+$((RETENTION_COUNT + 1))" || true
)

if ((${#OLD_DUMPS[@]} > 0)); then
  echo "$(date -Is) pruning ${#OLD_DUMPS[@]} old dump(s) (retention=${RETENTION_COUNT})"
  rm -f -- "${OLD_DUMPS[@]}"
fi

REMAINING="$(ls -1 "${BACKUP_DIR}"/kuky_*.sql.gz 2>/dev/null | wc -l | tr -d ' ')"
echo "$(date -Is) done — ${REMAINING} dump(s) retained in ${BACKUP_DIR}"
