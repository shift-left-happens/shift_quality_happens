# Load .env variables
include .env
export
# Windows-compatible command wrappers
ifeq ($(OS),Windows_NT)
MVNW := ./mvnw.cmd
OPEN := cmd /c start ""
TEST_DB_ENV := set "DB_URL=jdbc:mysql://localhost:3309/shift_happens?serverTimezone=UTC" &&
else
MVNW := ./mvnw
OPEN := open
TEST_DB_ENV := DB_URL=jdbc:mysql://localhost:3309/shift_happens?serverTimezone=UTC
endif


.PHONY: dev dev-db dev-frontend test-frontend dev-app dev-reset dev-down dev-clean dev-logs dev-shell verify lint lint-check test test-env-test test-unit test-one coverage test-env-up test-env-db test-env-down test-env-reset test-env-logs fe-test-install fe-test fe-test-api fe-test-e2e fe-test-one fe-test-headed fe-test-ui fe-test-report perf-smoke perf perf-one perf-dashboard-smoke perf-dashboard-load perf-dashboard-stress perf-dashboard-spike perf-dashboard-soak backup restore all all-tests

# ──────────────────────────────────────────────────────────────
# Orchestration
# ──────────────────────────────────────────────────────────────

## Start full stack (DB + backend + frontend)
all:
	@echo "Starting full stack with Docker Compose..."
	docker compose up --build -d --wait
	@echo "Starting frontend..."
	cd frontend && npm run dev

all-tests:
	@echo "Starting full stack for tests..."
	docker compose up --build -d --wait
	@echo "Running backend tests..."
	$(MVNW) test
	@echo "Running frontend tests..."
	cd frontend && npx playwright test
	@echo "Stopping stack..."

# Development : DB runs in Docker, app runs locally via Maven
# Test        : Full stack (DB + app) runs in Docker on separate ports

# ──────────────────────────────────────────────────────────────
# Development
# ──────────────────────────────────────────────────────────────

## Start dev DB + run the Spring Boot app locally
dev:
	docker compose up -d --wait db
	$(MVNW) spring-boot:run

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
	$(MVNW) spring-boot:run

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
	$(MVNW) spotless:apply

## Check formatting without making changes
lint-check:
	$(MVNW) spotless:check

## Run all tests against the dev DB
test: dev-db
	$(MVNW) test

## Run all tests against the isolated test environment (port 3309)
test-env-test:
	$(TEST_DB_ENV) $(MVNW) test

## Run all Java unit tests in src/test/java
test-unit:
	$(MVNW) test "-Dtest=**/*Test.java"

## Run a single test class. Usage: make test-one CLASS=EmployeeServiceTest
test-one:
	$(MVNW) test -Dtest=$(CLASS)

## Run tests + open JaCoCo coverage report
coverage: dev-db
	$(MVNW) test
	@$(OPEN) target/site/jacoco/index.html

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
# Frontend tests (Playwright) — needs a running backend.
# Default target backend is the isolated TEST stack on :8081 (start it with
# `make test-env-up`). To point at the dev backend instead, override API_URL,
# e.g.  API_URL=http://localhost:8080 make fe-test-e2e
# Vite proxies /api -> $(API_URL) and the specs read API_URL, so one var points
# both the app and the tests at the same backend.
# BASE_URL uses localhost (not 127.0.0.1) so Playwright's dev-server readiness
# check matches Vite's host on macOS/IPv6 — otherwise the run hangs at startup.
# Playwright auto-starts the frontend dev server (port 5173).
# ──────────────────────────────────────────────────────────────

## Install the Playwright browser (Chromium) — run once
fe-test-install:
	cd frontend && npx playwright install chromium

## Run all frontend tests (API + E2E)
fe-test:
	cd frontend && API_URL=$${API_URL:-http://localhost:8081} npx playwright test

## Run only the API specs (*.api.spec.ts)
fe-test-api:
	cd frontend && API_URL=$${API_URL:-http://localhost:8081} npx playwright test api.spec

## Run only the end-to-end specs (*.e2e.spec.ts)
fe-test-e2e:
	cd frontend && API_URL=$${API_URL:-http://localhost:8081} npx playwright test e2e.spec

## Run a single spec or filter by path/title. Usage: make fe-test-one SPEC=shiftapproval
fe-test-one:
	cd frontend && API_URL=$${API_URL:-http://localhost:8081} npx playwright test $(SPEC)

## Watch the E2E tests run in a visible browser (headed)
fe-test-headed:
	cd frontend && API_URL=$${API_URL:-http://localhost:8081} npx playwright test e2e.spec --headed

## Open Playwright's interactive UI — watch/replay tests in the browser, re-run on save
fe-test-ui:
	cd frontend && npx playwright test --ui

## Open the last Playwright HTML report
fe-test-report:
	cd frontend && npx playwright show-report

## Check frontend linting
fe-lint:
	cd frontend && npm run lint

## Auto-fix frontend lint violations
fe-lint-fix:
	cd frontend && npm run lint:fix

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

# ══════════════════════════════════════════════════════════════
# EXAM — run against the ISOLATED TEST STACK (docker-compose.test.yml:
# DB :3309, app :8081). Run one layer at a time, matching the
# black-box → unit → API → E2E → performance flow of the presentation.
#
#   make test-env-up           # 1. start the test stack (DB :3309 + app :8081)
#   make test-env-test         # 2. backend unit + integration tests (+ JaCoCo)
#   make coverage              #    open the JaCoCo coverage report
#   make fe-test-api           # 3. internal API tests        (targets :8081 automatically)
#   make fe-test-e2e           # 4. end-to-end UI tests        (all 10, targets :8081)
#   make fe-test-report        #    open the last Playwright HTML report
#   make perf-dashboard-load   # 5. a k6 scenario in the live web dashboard (:5665)
#   make test-env-down         #    stop the test stack
#
#   perf scenarios:  perf-dashboard-smoke / -load / -stress / -spike / -soak
#
# (test-env-up reuses the existing app image — no rebuild. If you ever need a
#  fresh image/seed: make test-env-reset.)
# ══════════════════════════════════════════════════════════════

# k6 with the live web dashboard (auto-opens http://127.0.0.1:5665) — one scenario each.
K6_DASH := K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_OPEN=true k6 run performance/scenarios

## EXAM: smoke scenario in the web dashboard
perf-dashboard-smoke:
	$(K6_DASH)/00-smoke-test.js
## EXAM: load scenario in the web dashboards
perf-dashboard-load:
	$(K6_DASH)/01-load-test.js
## EXAM: stress scenario in the web dashboard
perf-dashboard-stress:
	$(K6_DASH)/02-stress-test.js
## EXAM: spike scenario in the web dashboard
perf-dashboard-spike:
	$(K6_DASH)/03-spike-test.js
## EXAM: soak scenario in the web dashboard
perf-dashboard-soak:
	$(K6_DASH)/04-soak-test.js
