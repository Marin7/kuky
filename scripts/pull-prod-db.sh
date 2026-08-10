#!/usr/bin/env bash
# Dump Postgres from the production VM and restore into local kuky_dev.
#
# Prerequisites: ssh access to the VM, local psql, local DB kuky_dev (user kuky).
#
# Usage:
#   ./scripts/pull-prod-db.sh
#
# Env overrides:
#   SSH_HOST=root@81.27.101.239
#   REMOTE_DIR=/opt/kuky
#   LOCAL_HOST=localhost LOCAL_PORT=5432 LOCAL_DB=kuky_dev LOCAL_USER=kuky
#   PGPASSWORD=kuky

set -euo pipefail

SSH_HOST="${SSH_HOST:-root@81.27.101.239}"
REMOTE_DIR="${REMOTE_DIR:-/opt/kuky}"
LOCAL_HOST="${LOCAL_HOST:-localhost}"
LOCAL_PORT="${LOCAL_PORT:-5432}"
LOCAL_DB="${LOCAL_DB:-kuky_dev}"
LOCAL_USER="${LOCAL_USER:-kuky}"
export PGPASSWORD="${PGPASSWORD:-kuky}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DUMP_DIR="${DUMP_DIR:-${SCRIPT_DIR}/../backups}"
DUMP_FILE="${DUMP_DIR}/prod_$(date +%Y-%m-%d_%H%M%S).sql.gz"

mkdir -p "${DUMP_DIR}"

echo "==> dumping ${SSH_HOST}:${REMOTE_DIR} → ${DUMP_FILE}"
# accept-new: trust unknown hosts on first connect; still fail if a known key changes
ssh -o BatchMode=yes -o StrictHostKeyChecking=accept-new "${SSH_HOST}" \
  "cd '${REMOTE_DIR}' && docker compose exec -T postgres \
     pg_dump -U kuky -d kuky --clean --if-exists --no-owner --no-acl" \
  | gzip -c > "${DUMP_FILE}"

SIZE="$(wc -c < "${DUMP_FILE}" | tr -d ' ')"
if [[ "${SIZE}" -lt 100 ]]; then
  echo "error: dump is empty or too small (${SIZE} bytes)" >&2
  exit 1
fi
echo "==> dump ok (${SIZE} bytes)"

echo "==> terminating local connections to ${LOCAL_DB}"
psql -h "${LOCAL_HOST}" -p "${LOCAL_PORT}" -U "${LOCAL_USER}" -d postgres -v ON_ERROR_STOP=1 <<SQL
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = '${LOCAL_DB}' AND pid <> pg_backend_pid();
SQL

echo "==> restoring into ${LOCAL_DB}"
gunzip -c "${DUMP_FILE}" \
  | psql -h "${LOCAL_HOST}" -p "${LOCAL_PORT}" -U "${LOCAL_USER}" -d "${LOCAL_DB}" -v ON_ERROR_STOP=1

echo "==> done — ${DUMP_FILE} applied to ${LOCAL_DB}"
