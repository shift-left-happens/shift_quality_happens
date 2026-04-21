# Load .env variables
include .env
export

# ──────────────────────────────────────────────────────────────
# Development
# ──────────────────────────────────────────────────────────────

## Start all 3 databases in the background, then run the Spring Boot app locally
run-all:
	docker compose up -d --wait db mongodb neo4j
	@echo "MySQL is ready."
	./mvnw spring-boot:run

## Start all 3 databases in the background only (no app)
run-dbs:
	docker compose up -d --wait db mongodb neo4j

## Run the Spring Boot app locally (databases must already be running)
run-app:
	./mvnw spring-boot:run

## Start only MySQL (watch logs for errors)
db:
	docker compose up db

## Nuke all volumes and restart databases fresh (re-runs all init scripts)
reset:
	docker compose down -v
	docker compose up -d --wait db mongodb neo4j

## Stop everything
down:
	docker compose down

## Stop everything and delete all data volumes
clean:
	docker compose down -v

## Show MySQL logs
db-logs:
	docker compose logs db

## Connect to MySQL CLI inside the container
db-shell:
	docker exec -it shift-happens-db mysql -u root -p$(MYSQL_ROOT_PASSWORD) $(MYSQL_DATABASE)

# ──────────────────────────────────────────────────────────────
# Load Dumps  (restore committed seed data into live containers)
# ──────────────────────────────────────────────────────────────

## Load both committed dumps — run after: make reset && make run-dbs
load-dbs:
	@bash src/main/resources/db/mongodb/load.sh
	@bash src/main/resources/db/neo4j/load.sh

## Load the committed MongoDB dump only
load-mongo:
	@bash src/main/resources/db/mongodb/load.sh

## Load the committed Neo4j dump only
load-neo4j:
	@bash src/main/resources/db/neo4j/load.sh

# ──────────────────────────────────────────────────────────────
# Backups
# ──────────────────────────────────────────────────────────────

## Dump all 3 databases to backups/<timestamp>/
backup:
	@bash scripts/backup.sh

## Restore from a backup. Usage: make restore BACKUP=backups/<timestamp>
restore:
	@bash scripts/restore.sh $(BACKUP)

## Print record counts across all 3 databases
verify:
	@echo "=== MySQL ==="
	@docker exec shift-happens-db mysql -u root -p$(MYSQL_ROOT_PASSWORD) -sN \
		-e "SELECT COUNT(*) FROM $(MYSQL_DATABASE).employee;" 2>/dev/null
	@echo "=== MongoDB ==="
	@docker exec shift-happens-mongo mongosh shift_happens --quiet \
		--eval "db.employees.countDocuments()" 2>/dev/null
	@echo "=== Neo4j ==="
	@docker exec shift-happens-neo4j cypher-shell \
		-u neo4j -p $(NEO4J_PASSWORD) \
		"MATCH (n) RETURN labels(n)[0] AS label, count(n) AS count ORDER BY label;" 2>/dev/null
