# Shift Happens

A shift management system developed throughout our Database and Software Quality course. The project evolves across multiple phases, each introducing a new database technology while keeping Spring Boot as the core framework.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/)

## Installation & Setup

### 1. Clone the repository

```bash
git clone https://github.com/Luke3520/shift_happens.git
cd shift-happens
```

### 2. Configure environment

```bash
cp .env.example .env
```

Fill in your credentials in `.env`. The Docker defaults work out of the box.

### 3. Start the application

```bash
make up
```

This single command will:
- Start a **MySQL 8.0** container with the database `shift_happens`
- Run all SQL init scripts automatically (schema, seed data, views, routines, triggers, events)
- Build the **Spring Boot** application from source
- Start the API on **http://localhost:8080**

The database is exposed on **localhost:3307** (to avoid conflicts with any local MySQL on 3306).

### 4. Verify it works

Once the logs show `Started ShiftHappensApplication`, the API is ready:

```bash
curl http://localhost:8080/employees
```

### 5. Stop the application

```bash
make down
```

To also remove the database volume (full reset):

```bash
make clean
```

## Make Commands

| Command | Description |
|---------|-------------|
| `make up` | Start everything (DB + app) |
| `make db` | Start only the database |
| `make app` | Start only the app (rebuilds jar) |
| `make reset` | Nuke DB volume and restart fresh (re-runs all init scripts) |
| `make down` | Stop everything |
| `make clean` | Stop everything and delete DB data |
| `make db-logs` | Show DB logs |
| `make db-shell` | Connect to MySQL CLI inside the container |

## Connecting to the Database

### CLI

```bash
make db-shell
```

### GUI (MySQL Workbench)

| Setting | Value |
|---------|-------|
| Host | `127.0.0.1` |
| Port | `3307` |
| User | See `DB_USERNAME` in `.env` |
| Password | See `MYSQL_ROOT_PASSWORD` in `.env` |
| Database | See `MYSQL_DATABASE` in `.env` |

## Project Structure

```
shift-happens/
├── docker/
│   └── init/                  # SQL scripts run on first DB startup
│       ├── 01-schema.sql      # All 19 tables, indexes, constraints
│       ├── 02-seed-data.sql   # 100 rows per table (test data)
│       ├── 03-views.sql       # Database views
│       ├── 04-routines.sql    # Stored procedures and functions
│       ├── 05-triggers.sql    # Leave-approval trigger
│       └── 06-events.sql      # Scheduled events
├── src/main/
│   ├── java/dk/ek/shift_happens/   # Spring Boot application
│   └── resources/
│       ├── application.properties
│       └── queries/            # Original SQL scripts (reference)
├── .env.example              # Template for environment variables
├── docker-compose.yml
├── Dockerfile
├── Makefile
└── pom.xml
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.5 (Java 21) |
| Build Tool | Maven |
| Phase 1 DB | MySQL 8.0 + Spring Data JPA |
| Phase 2 DB | MongoDB + Spring Data MongoDB (Coming soon) |
| Phase 3 DB | Neo4j + Spring Data Neo4j (Coming soon) |
| Unit Testing | JUnit 5 + Mockito (Coming soon) |
| Integration Testing | Testcontainers (Coming soon) |
| Other | Lombok, Docker & Docker Compose |
