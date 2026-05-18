# Load .env variables
include .env
export

# Development : DB runs in Docker, app runs locally via Maven
# Test        : Full stack (DB + app) runs in Docker on separate ports

# ──────────────────────────────────────────────────────────────
# Development
# ──────────────────────────────────────────────────────────────

## Start dev DB + run the Spring Boot app locally
dev:
	docker compose up -d --wait db
	./mvnw spring-boot:run

## Start dev DB in the background only
dev-db:
	docker compose up -d --wait db

## Run the frontend pointed at the dev backend (port 8080)
dev-frontend:
	cd frontend && npm run dev:prod

## Run the frontend pointed at the test backend (port 8081)
test-frontend:
	cd frontend && npm run dev:test

## Run the Spring Boot app locally (dev DB must already be running)
dev-app:
	./mvnw spring-boot:run

## Wipe the dev DB volume and restart fresh (re-runs all init scripts)
dev-reset:
	docker compose down -v
	docker compose up -d --wait db

## Stop the dev environment
dev-down:
	docker compose down

## Stop dev environment and delete the data volume
dev-clean:
	docker compose down -v

## Show dev DB logs
dev-logs:
	docker compose logs -f db

## Connect to dev MySQL CLI
dev-shell:
	docker exec -it shift-quality-happens-db mysql -u root -p$(MYSQL_ROOT_PASSWORD) $(MYSQL_DATABASE)

## Print employee row count in dev DB
verify:
	@echo "=== MySQL (dev) ==="
	@docker exec shift-quality-happens-db mysql -u root -p$(MYSQL_ROOT_PASSWORD) -sN \
		-e "SELECT COUNT(*) FROM $(MYSQL_DATABASE).employee;" 2>/dev/null

# ──────────────────────────────────────────────────────────────
# Tests
# ──────────────────────────────────────────────────────────────

## Auto-fix formatting with Spotless (Palantir Java Format)
lint:
	./mvnw spotless:apply

## Check formatting without making changes
lint-check:
	./mvnw spotless:check

## Run all tests against the dev DB
test:
	./mvnw test

## Run all tests against the isolated test environment (port 3309)
test-env-test:
	DB_URL=jdbc:mysql://localhost:3309/shift_happens?serverTimezone=UTC ./mvnw test

## Run only the blackbox unit tests (no Spring context, no DB needed)
test-unit:
	./mvnw test -Dtest='EmployeeServiceTest,ShiftServiceTest,ShiftAssignmentServiceTest,ShiftSwapApprovalServiceTest,JobRoleServiceTest'

## Run a single test class. Usage: make test-one CLASS=EmployeeServiceTest
test-one:
	./mvnw test -Dtest=$(CLASS)

## Run tests + open JaCoCo coverage report
coverage:
	./mvnw test
	@open target/site/jacoco/index.html

# ──────────────────────────────────────────────────────────────
# Test Environment (full Docker stack — DB on 3309, app on 8081)
# ──────────────────────────────────────────────────────────────

## Start the full test stack (DB + app) in the background
test-env-up:
	docker compose -f docker-compose.test.yml --env-file .env.test up -d --wait

## Start only the test DB (useful when running the app locally against test DB)
test-env-db:
	docker compose -f docker-compose.test.yml --env-file .env.test up -d --wait db

## Stop the test environment
test-env-down:
	docker compose -f docker-compose.test.yml --env-file .env.test down

## Wipe the test volume and restart the full stack fresh
test-env-reset:
	docker compose -f docker-compose.test.yml --env-file .env.test down -v
	docker compose -f docker-compose.test.yml --env-file .env.test up -d --wait

## Show test environment logs
test-env-logs:
	docker compose -f docker-compose.test.yml --env-file .env.test logs

# ──────────────────────────────────────────────────────────────
# Performance (run against test env — start with make test-env-up first)
# ──────────────────────────────────────────────────────────────

## Quick sanity check (1 VU, 1 iteration)
perf-smoke:
	k6 run performance/scenarios/00-smoke-test.js

## Run all k6 performance tests sequentially
perf:
	k6 run performance/scenarios/00-smoke-test.js
	k6 run performance/scenarios/01-load-test.js
	k6 run performance/scenarios/02-stress-test.js
	k6 run performance/scenarios/03-spike-test.js
	k6 run performance/scenarios/04-soak-test.js

## Run a single performance test. Usage: make perf-one TEST=01-load-test
perf-one:
	k6 run performance/scenarios/$(TEST).js

# ──────────────────────────────────────────────────────────────
# Backups
# ──────────────────────────────────────────────────────────────

## Dump the MySQL database to backups/<timestamp>/
backup:
	@bash scripts/backup.sh

## Restore from a backup. Usage: make restore BACKUP=backups/<timestamp>
restore:
	@bash scripts/restore.sh $(BACKUP)
