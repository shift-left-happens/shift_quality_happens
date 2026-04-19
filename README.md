# Shift Happens

A shift scheduling and leave management system built as a database course project. The system evolves across three database phases — relational, document, and graph — all running simultaneously via a one-time migration.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Framework | Spring Boot 3.5 (Java 21) |
| Build | Maven |
| Phase 1 | MySQL 8.0 + Spring Data JPA |
| Phase 2 | MongoDB + Spring Data MongoDB |
| Phase 3 | Neo4j + Spring Data Neo4j |
| Auth | Spring Security + JWT (HMAC-SHA256) |
| Other | Lombok, Docker, Docker Compose |

---

## Installation & Setup

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/)
- Java 21 (only needed if running the app outside Docker)

### 1. Clone the repository

```bash
git clone https://github.com/Luke3520/shift_happens.git
cd shift_happens
```

### 2. Configure environment

```bash
cp .env.example .env
```

The `.env.example` file contains working defaults for local development — no changes required to get started.

### 3. Start everything with Docker Compose

```bash
docker compose up app
```

This single command starts MySQL, MongoDB, Neo4j, and the Spring Boot application. All databases are initialized automatically from the SQL scripts in `docker/init/`.

The API will be available at **http://localhost:8080** once you see:
```
Started ShiftHappensApplication in X.XXX seconds
```

To stop:
```bash
docker compose down
```

To wipe all data and start fresh:
```bash
docker compose down -v
docker compose up app
```

### 4. Populate MongoDB and Neo4j

After the app is running, trigger the one-time migration from MySQL:

```bash
curl -X POST http://localhost:8080/migrate
```

This reads all MySQL data and writes it into MongoDB and Neo4j. You only need to run this once (or after a `docker compose down -v` reset).

---

## Make Commands (optional shortcut — requires `make`)

If you have `make` installed, these wrap the docker/maven commands above:

| Command | Equivalent |
|---------|------------|
| `make run-all` | Start all 3 DBs + run app locally (outside Docker) |
| `make run-dbs` | `docker compose up -d --wait db mongodb neo4j` |
| `make run-app` | `./mvnw spring-boot:run` |
| `make reset` | `docker compose down -v && docker compose up -d --wait db mongodb neo4j` |
| `make down` | `docker compose down` |
| `make clean` | `docker compose down -v` |
| `make db-shell` | Open MySQL CLI inside the container |

---

## Authentication

All endpoints except `POST /auth/login` require a JWT bearer token.

**Login:**
```
POST /auth/login
Content-Type: application/json

{ "email": "user@example.com", "password": "yourpassword" }
```

**Response:**
```json
{ "token": "eyJ...", "employeeId": 1, "role": "ADMINISTRATOR", ... }
```

**Using the token:**
```
Authorization: Bearer <token>
```

**Role-based access:**
- `GET /**` — any authenticated user
- `POST / PUT / PATCH / DELETE /**` — ADMINISTRATOR or MANAGER only
- `GET /audit-log/**` — ADMINISTRATOR only

---

## API Endpoints

### MySQL

| Resource | Path |
|----------|------|
| Auth | `POST /auth/login` (public) |
| Employees | `/employees` |
| Departments | `/departments` |
| Work Locations | `/work-locations` |
| Shifts | `/shifts` |
| Shift Assignments | `/shift-assignments` |
| Shift Approvals | `/shift-approvals` |
| Shift Swaps | `/shift-swaps` |
| Shift Swap Approvals | `/shift-swap-approvals` |
| Leave Types | `/leave-types` |
| Leave Requests | `/leave-requests` |
| Leave Approvals | `/leave-approvals` |
| Leave Ledger | `/leave-ledger` |
| Job Roles | `/job-roles` |
| Employee Job Roles | `/employee-job-roles` |
| Employee Contracts | `/employee-contracts` |
| User Roles | `/user-roles` |
| Audit Log | `/audit-log` |

### MongoDB (requires migration)

| Resource | Path |
|----------|------|
| Employees | `/mongo/employees` |
| Shifts | `/mongo/shifts` |
| Departments | `/mongo/departments` |
| Job Roles | `/mongo/job_role` |
| Leave Types | `/mongo/leave_type` |
| Leave | `/mongo/leave` |
| Audit Log | `/mongo/audit_log` |
| User Roles | `/mongo/user_role` |
| Work Locations | `/mongo/work_location` |

All MongoDB endpoints support `GET`, `POST`, `PUT`, and `DELETE`.

### Neo4j (requires migration)

| Resource | Path |
|----------|------|
| Employees | `/neo4j/employees` |
| Departments | `/neo4j/departments` |
| Shifts | `/neo4j/shifts` |
| Job Roles | `/neo4j/job_role` |
| Work Locations | `/neo4j/work_location` |

### Migration

| Endpoint | Description |
|----------|-------------|
| `POST /migrate` | Full migration: MySQL → MongoDB + Neo4j |
| `POST /migrate/mongo` | MySQL → MongoDB only |
| `POST /migrate/neo4j` | MySQL → Neo4j only |

---

## Database Schema (MySQL)

**18 tables** across four domains:

**Organisation:** `department`, `work_location`, `user_role`

**People:** `employee`, `employee_contract`, `job_role`, `employee_job_role`

**Scheduling:** `shift`, `shift_required_job_role`, `shift_assignment`, `shift_approval`, `shift_swap`, `shift_swap_approval`

**Leave:** `leave_type`, `leave_request`, `leave_approval`, `leave_ledger`

**Auditing:** `audit_log`

Notable constraints:
- `leave_approval` inserts are validated by a DB trigger that checks manager role and leave balance
- `leave_ledger` tracks balances double-entry style (ACCRUALs +, USAGEs −)
- Composite PKs on `employee_job_role` and `shift_required_job_role` use `@IdClass`

---

## MongoDB Design

Employee and Shift documents are **denormalized** — related data is embedded rather than referenced:

- **Employee** embeds: department, work location, user role, contracts, job roles, leave requests with approvals, leave ledger entries
- **Shift** embeds: required job roles, assignments with approvals and swap requests

Reference data (Department, JobRole, LeaveType, UserRole, WorkLocation, AuditLog) is kept flat.

See [`src/main/resources/document_prototypes/`](src/main/resources/document_prototypes/) for the full document shapes.

---

## Connecting to the Databases

**MySQL CLI:**
```bash
make db-shell
# or
docker exec -it shift-happens-db mysql -u root -prootpassword shift_happens
```

**MySQL GUI (Workbench, DBeaver, etc.):**

| Setting | Value |
|---------|-------|
| Host | `127.0.0.1` |
| Port | `3307` |
| User | `root` |
| Password | `MYSQL_ROOT_PASSWORD` from `.env` |
| Database | `shift_happens` |

**MongoDB:** `mongodb://localhost:27017/shift_happens`

**Neo4j Browser:** http://localhost:7474 (bolt port: `7687`)

---

## User Stories

| Role | Story |
|------|-------|
| Admin | Create and manage employees, departments, work locations |
| Admin | Build schedules and approve/decline shift assignments |
| Admin | Approve/decline shift swap requests |
| Manager | Approve/decline leave requests |
| Employee | View assigned schedule |
| Employee | Apply for a shift or leave |
| Employee | Request a shift swap with another employee |
