#!/usr/bin/env bash
# Loads the committed Neo4j dump into the running shift-happens-neo4j container.
# Usage: bash src/main/resources/db/neo4j/load.sh
# Run from anywhere — the script resolves its own path.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"
cd "$REPO_ROOT"

set -a; source .env; set +a

GREEN='\033[0;32m'; BOLD='\033[1m'; NC='\033[0m'

[[ -f "$SCRIPT_DIR/neo4j.dump" ]] || { echo "No dump found at $SCRIPT_DIR/neo4j.dump. Generate one with: bash scripts/backup.sh"; exit 1; }

echo "Loading Neo4j dump (requires brief stop)..."

docker compose stop neo4j

docker compose run --rm --no-deps \
  -v "$SCRIPT_DIR:/backup" \
  --entrypoint neo4j-admin \
  neo4j \
  database load neo4j --from-path=/backup --overwrite-destination

docker compose start neo4j

echo "Waiting for Neo4j to be ready..."
for i in $(seq 1 12); do
  if docker exec shift-happens-neo4j cypher-shell \
      -u neo4j -p "${NEO4J_PASSWORD}" "RETURN 1;" >/dev/null 2>&1; then
    break
  fi
  [[ $i -eq 12 ]] && { echo "Neo4j did not become ready in time"; exit 1; }
  sleep 5
done

NODE_COUNT=$(docker exec shift-happens-neo4j cypher-shell \
  -u neo4j -p "${NEO4J_PASSWORD}" \
  "MATCH (n) RETURN count(n) AS count;" 2>/dev/null \
  | grep -oE '[0-9]+' | tail -1)

echo -e "${GREEN}${BOLD}Neo4j loaded — $NODE_COUNT nodes.${NC}"
