# Load .env variables
include .env
export

# ──────────────────────────────────────────────────────────────
# Development
# ──────────────────────────────────────────────────────────────

## Start MySQL in the background, then run the Spring Boot app locally
run-all:
	docker compose up -d --wait db
	@echo "MySQL is ready."
	./mvnw spring-boot:run

## Start MySQL in the background only (no app)
run-dbs:
	docker compose up -d --wait db

## Run the Spring Boot app locally (database must already be running)
run-app:
	./mvnw spring-boot:run

## Start only MySQL (watch logs for errors)
db:
	docker compose up db

## Nuke the volume and restart MySQL fresh (re-runs all init scripts)
reset:
	docker compose down -v
	docker compose up -d --wait db

## Stop everything
down:
	docker compose down

## Stop everything and delete the data volume
clean:
	docker compose down -v

## Show MySQL logs
db-logs:
	docker compose logs db

## Connect to MySQL CLI inside the container
db-shell:
	docker exec -it shift-quality-happens-db mysql -u root -p$(MYSQL_ROOT_PASSWORD) $(MYSQL_DATABASE)

# ──────────────────────────────────────────────────────────────
# Tests
# ──────────────────────────────────────────────────────────────

## Run all unit tests (requires MySQL running for ShiftHappensApplicationTests.contextLoads)
test:
	./mvnw test

## Run only the blackbox-derived unit tests (no Spring context, no DB needed)
test-unit:
	./mvnw test -Dtest='EmployeeServiceTest,ShiftServiceTest,ShiftAssignmentServiceTest,ShiftSwapApprovalServiceTest,JobRoleServiceTest'

## Run a single test class. Usage: make test-one CLASS=EmployeeServiceTest
test-one:
	./mvnw test -Dtest=$(CLASS)

## Open the JaCoCo coverage report in the default browser
coverage:
	./mvnw test
	@open target/site/jacoco/index.html

# ──────────────────────────────────────────────────────────────
# Backups
# ──────────────────────────────────────────────────────────────

## Dump the MySQL database to backups/<timestamp>/
backup:
	@bash scripts/backup.sh

## Restore from a backup. Usage: make restore BACKUP=backups/<timestamp>
restore:
	@bash scripts/restore.sh $(BACKUP)

## Print employee row count
verify:
	@echo "=== MySQL ==="
	@docker exec shift-quality-happens-db mysql -u root -p$(MYSQL_ROOT_PASSWORD) -sN \
		-e "SELECT COUNT(*) FROM $(MYSQL_DATABASE).employee;" 2>/dev/null
