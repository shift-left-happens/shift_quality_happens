#!/usr/bin/env bash
# Loads the committed MongoDB dump into the running shift-happens-mongo container.
# Usage: bash src/main/resources/db/mongodb/load.sh
# Run from anywhere — the script resolves its own path.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DUMP_DIR="$SCRIPT_DIR/dump"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"
cd "$REPO_ROOT"

GREEN='\033[0;32m'; BOLD='\033[1m'; NC='\033[0m'

[[ -d "$DUMP_DIR" ]] || { echo "No dump found at $DUMP_DIR. Generate one with: bash scripts/backup.sh"; exit 1; }

echo "Loading MongoDB dump from $DUMP_DIR..."

docker cp "$DUMP_DIR" shift-happens-mongo:/tmp/mongorestore_src

docker exec shift-happens-mongo mongorestore \
  --db shift_happens --drop /tmp/mongorestore_src --quiet

docker exec shift-happens-mongo rm -rf /tmp/mongorestore_src

DOC_COUNT=$(docker exec shift-happens-mongo mongosh shift_happens --quiet \
  --eval "db.employees.countDocuments()" 2>/dev/null | tail -1)

echo -e "${GREEN}${BOLD}MongoDB loaded — $DOC_COUNT employee documents.${NC}"
