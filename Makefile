# Load .env variables
include .env
export

# Shift Happens - Docker commands

## Start all 3 databases in the background, then run the Spring Boot app locally
run-all:
	docker compose up -d db mongodb neo4j
	@echo "Waiting for MySQL to be healthy..."
	@until [ "$$(docker inspect --format='{{.State.Health.Status}}' shift-happens-db)" = "healthy" ]; do sleep 2; done
	@echo "MySQL is ready."
	./mvnw spring-boot:run

## Start all 3 databases in the background only (no app)
run-dbs:
	docker compose up -d db mongodb neo4j

## Run the Spring Boot app locally (databases must already be running)
run-app:
	./mvnw spring-boot:run

## Start only MySQL (watch logs for errors)
db:
	docker compose up db

## Nuke all volumes and restart databases fresh (re-runs all init scripts)
reset:
	docker compose down -v
	docker compose up -d db mongodb neo4j

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
