# Load .env variables
include .env
export

# Shift Happens - Docker commands

## Start everything (DB + app)
up:
	docker compose up

## Start only the database (watch logs for errors)
db:
	docker compose up db

## Start only the app (rebuilds jar)
app:
	docker compose up app --build

## Nuke DB volume and restart fresh (re-runs all init scripts)
reset:
	docker compose down -v
	docker compose up db

## Stop everything
down:
	docker compose down

## Stop everything and delete DB data
clean:
	docker compose down -v

## Show DB logs
db-logs:
	docker compose logs db

## Connect to MySQL CLI inside the container
db-shell:
	docker exec -it shift-happens-db mysql -u root -p$(MYSQL_ROOT_PASSWORD) $(MYSQL_DATABASE)
