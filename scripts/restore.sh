#!/usr/bin/env bash
# Usage: bash scripts/restore.sh backups/<timestamp>
set -euo pipefail

BACKUP_DIR="${1:-}"
if [[ -z "$BACKUP_DIR" ]]; then
  echo "Usage: bash scripts/restore.sh backups/<timestamp>"
  echo ""
  echo "Available backups:"
  ls -1 backups/ 2>/dev/null || echo "  (none found)"
  exit 1
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# Resolve to absolute path
BACKUP_DIR="$(cd "$BACKUP_DIR" && pwd)"

# Load env vars
set -a; source .env; set +a

RED='\033[0;31m'; GREEN='\033[0;32m'; BOLD='\033[1m'; NC='\033[0m'

log()  { echo -e "$1"; }
ok()   { log "${GREEN}  ✓ $1${NC}"; }
fail() { log "${RED}  ✗ $1${NC}"; exit 1; }

log "${BOLD}=== Shift Happens Restore ===${NC}"
log "    Source: $BACKUP_DIR"
log ""

[[ -f "$BACKUP_DIR/mysql.sql" ]] || fail "mysql.sql not found in backup"

log "[1/1] MySQL..."
docker exec shift-quality-happens-db mysql \
  -u root -p"${MYSQL_ROOT_PASSWORD}" \
  -e "DROP DATABASE IF EXISTS \`${MYSQL_DATABASE}\`; CREATE DATABASE \`${MYSQL_DATABASE}\`;" 2>/dev/null

docker exec -i shift-quality-happens-db mysql \
  -u root -p"${MYSQL_ROOT_PASSWORD}" \
  "${MYSQL_DATABASE}" < "$BACKUP_DIR/mysql.sql" 2>/dev/null

ROW_COUNT=$(docker exec shift-quality-happens-db mysql \
  -u root -p"${MYSQL_ROOT_PASSWORD}" -sN \
  -e "SELECT COUNT(*) FROM \`${MYSQL_DATABASE}\`.employee;" 2>/dev/null)
ok "MySQL restored — $ROW_COUNT employee rows"

log ""
log "${GREEN}${BOLD}Restore complete.${NC}"
log "  MySQL     $ROW_COUNT employee rows"
